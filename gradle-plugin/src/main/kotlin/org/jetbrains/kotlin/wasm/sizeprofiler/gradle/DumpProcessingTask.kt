package org.jetbrains.kotlin.wasm.sizeprofiler.gradle

import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.wasm.sizeprofiler.core.EdgeEntry
import org.jetbrains.kotlin.wasm.sizeprofiler.core.execution.DominatorsExecutor
import org.jetbrains.kotlin.wasm.sizeprofiler.core.execution.FilterExecutor
import org.jetbrains.kotlin.wasm.sizeprofiler.core.graph.VertexWithType


abstract class DumpProcessingTask : DefaultTask() {
    @get:InputDirectory
    abstract val inputDir: DirectoryProperty

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

        if (!irSizesFile.exists()) {
            error("IR sizes file is missing. Visualization couldn't be made")
        }
        if (!dceGraphFile.exists()) {
            error("DCE graph data is missing. Visualization couldn't be made")
        }
        val edges = Json.decodeFromString<List<EdgeEntry>>(dceGraphFile.readText()).let {
            if (irSizesExtendedFile.exists()) {
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

    }

    private fun filterEdges(extendedIrSizes: Map<String, VertexWithType>, edges: List<EdgeEntry>) =
        FilterExecutor(extendedIrSizes, edges).newEdges

    private fun buildDominatorTree(irSizes: Map<String, VertexWithType>, edges: List<EdgeEntry>) =
        DominatorsExecutor(edges, irSizes, false)
}