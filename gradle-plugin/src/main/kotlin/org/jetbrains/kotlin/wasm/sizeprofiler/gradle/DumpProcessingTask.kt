package org.jetbrains.kotlin.wasm.sizeprofiler.gradle

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.wasm.sizeprofiler.core.EdgeEntry
import org.jetbrains.kotlin.wasm.sizeprofiler.core.execution.BuildMappingExecutor
import org.jetbrains.kotlin.wasm.sizeprofiler.core.execution.DominatorsExecutor
import org.jetbrains.kotlin.wasm.sizeprofiler.core.execution.FilterExecutor
import org.jetbrains.kotlin.wasm.sizeprofiler.core.graph.VertexWithType
import org.jetbrains.kotlin.wasm.sizeprofiler.core.sourcemaps.SourceMapFile
import java.io.File


abstract class DumpProcessingTask : DefaultTask() {
    @get:InputDirectory
    abstract val inputDir: DirectoryProperty

    private val visualizationLocation = project.buildDir.resolve(VisualizationDownloaderTask.APP_LOCATION)
    private val profileDataLocation = visualizationLocation.resolve("profile-data")
    private val sourceMapsLocation = visualizationLocation.resolve("source-maps")

    @TaskAction
    fun processDumps() {
        val fileName = "${project.name}-wasm"
        val inputDirFile = inputDir.get().asFile
        val irSizesFile = inputDirFile.resolve("ir-sizes.json")
        val irSizesExtendedFile = inputDirFile.resolve("ir-sizes-extended.json")
        val dceGraphFile = inputDirFile.resolve("dce-graph.json")
        val sourceMapFile = inputDirFile.resolve("$fileName.map")
        val watSourceMapFile = inputDirFile.resolve("$fileName.wat.map")
        val watFile = inputDirFile.resolve("$fileName.wat")
        val functionLocationsFile = inputDirFile.resolve("functions-wat.json")

        logger.lifecycle("DUMP PROCESSING INPUT DIRECTORY: ${inputDir.get()}")
        logger.lifecycle("IR Sizes: $irSizesFile. Exists? ${irSizesFile.exists()}")
        logger.lifecycle("IR Sizes extended: $irSizesExtendedFile. Exists? ${irSizesExtendedFile.exists()}")
        logger.lifecycle("DCE Graph: $dceGraphFile. Exists? ${dceGraphFile.exists()}")
        logger.lifecycle("sourcemap kt-wasm: $sourceMapFile. Exists? ${sourceMapFile.exists()}")
        logger.lifecycle("sourcemap wat-wasm: $watSourceMapFile. Exists? ${watSourceMapFile.exists()}")
        logger.lifecycle("wat file: $watFile. Exists? ${watFile.exists()}")
        logger.lifecycle("function locations: $functionLocationsFile. Exists? ${functionLocationsFile.exists()}")

        transformGraph(irSizesFile, dceGraphFile, irSizesExtendedFile)
        if (watFile.exists() && sourceMapFile.exists() && watSourceMapFile.exists() && functionLocationsFile.exists()) {
            dumpSourceMaps(sourceMapFile, watSourceMapFile, functionLocationsFile)
        }
    }

    private fun transformGraph(irSizesFile: File, dceGraphFile: File, irSizesExtendedFile: File) {
        if (!irSizesFile.exists()) {
            error("IR sizes file is missing. Visualization couldn't be made")
        }
        if (!dceGraphFile.exists()) {
            error("DCE graph data is missing. Visualization couldn't be made")
        }
        val edges = Json.decodeFromString<List<EdgeEntry>>(dceGraphFile.readText()).let {
            if (irSizesExtendedFile.exists()) {
                logger.lifecycle("Compressing edges...")
                val extendedIrSizes = Json.decodeFromString<Map<String, VertexWithType>>(irSizesFile.readText())
                    .onEach { (k, v) -> v.name = k }
                filterEdges(extendedIrSizes, it)
            } else {
                it
            }
        }

        val irSizes = Json.decodeFromString<Map<String, VertexWithType>>(irSizesFile.readText())
            .onEach { (k, v) -> v.name = k }.fixDisplayName().restoreClassSizes(edges)
        val dominatorsTreeExecutor = buildDominatorTree(irSizes, edges)

        createNecessaryDirectories()
        dumpGraphLeft(edges, irSizes)
        dumpRetainedLeft(dominatorsTreeExecutor)
    }

    private fun filterEdges(extendedIrSizes: Map<String, VertexWithType>, edges: List<EdgeEntry>) =
        FilterExecutor(extendedIrSizes, edges).newEdges

    private fun buildDominatorTree(irSizes: Map<String, VertexWithType>, edges: List<EdgeEntry>) =
        DominatorsExecutor(edges, irSizes, false)

    private fun createNecessaryDirectories() {
        profileDataLocation.resolve("left-graph").mkdirs()
        profileDataLocation.resolve("retained-left").mkdirs()
        sourceMapsLocation.mkdirs()
    }

    private fun dumpGraphLeft(edges: List<EdgeEntry>, irSizes: Map<String, VertexWithType>) {
        val graphLeftLocation = profileDataLocation.resolve("left-graph")
        graphLeftLocation
            .resolve("ir-sizes.json")
            .writeText(Json.encodeToString(irSizes))
        graphLeftLocation
            .resolve("dce-graph.json")
            .writeText(Json.encodeToString(edges))
    }

    private fun dumpRetainedLeft(executor: DominatorsExecutor) {
        val retainedLeftLocation = profileDataLocation.resolve("retained-left")
        retainedLeftLocation
            .resolve("retained-sizes.json")
            .writeText(Json.encodeToString(executor.retainedSizes))
    }

    private fun dumpSourceMaps(sourceMapFile: File, watSourceMapFile: File, functionLocationsFile: File) {
        logger.lifecycle("Dump sourcemaps...")
        val sourceMap = Json
            .decodeFromString<SourceMapFile>(sourceMapFile.readText())
            .fixPaths(sourceMapFile.parentFile.toPath(), forKotlin = true)
        val watSourceMap = Json
            .decodeFromString<SourceMapFile>(watSourceMapFile.readText())
            .fixPaths(watSourceMapFile.parentFile.toPath(), forKotlin = false)
        val sourceMapSegments = sourceMap.buildSegments(quiet = true)
        val watSourceMapSegments = watSourceMap.buildSegments(quiet = true)
        val executor = BuildMappingExecutor(
            kotlinSegments = sourceMapSegments,
            watSegments = watSourceMapSegments
        )

        val kotlinDump = Json
            .decodeFromString<SourceMapDump>(sourceMapFile.readText())
            .fixPaths(sourceMapFile.parentFile.toPath(), forKotlin = true)
        val watDump = Json
            .decodeFromString<SourceMapDump>(watSourceMapFile.readText())
            .fixPaths(watSourceMapFile.parentFile.toPath(), forKotlin = false)

        sourceMapsLocation
            .resolve("kotlin.map")
            .writeText(Json.encodeToString(kotlinDump))
        sourceMapsLocation
            .resolve("wat.map")
            .writeText(Json.encodeToString(watDump))
        sourceMapsLocation
            .resolve("segments.json")
            .writeText(Json.encodeToString(executor.matchedSegments))
        functionLocationsFile.copyTo(
            target = sourceMapsLocation.resolve("functions-wat.json"),
            overwrite = true
        )
    }

    @Serializable
    internal data class SourceMapDump(
        val version: Int,
        val file: String? = null,
        val sourceRoot: String? = null,
        val sources: List<String>,
        val sourcesContent: List<String?>? = null,
        val names: List<String>,
        val mappings: String
    )

}