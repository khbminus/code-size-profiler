package org.jetbrains.kotlin.wasm.sizeprofiler.tasks

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.wasm.sizeprofiler.core.execution.BuildMappingExecutor
import org.jetbrains.kotlin.wasm.sizeprofiler.core.sourcemaps.SourceMapFile
import org.jetbrains.kotlin.wasm.sizeprofiler.core.sourcemaps.SourceMapSegment
import java.nio.file.Path
import kotlin.io.path.readText

val sourceMaps = SourceMaps().subcommands(GetSegments(), BuildKotlinWatSegments())

class SourceMaps : CliktCommand(help = "Read and process sourcemap", invokeWithoutSubcommand = false) {
    override fun run() = Unit
}

class GetSegments : CliktCommand(help = "Read and process sourcemap") {
    private val sourceMapFile: Path by argument("<path to wasm sourcemap file>").path(
        mustExist = true,
        mustBeReadable = true,
        canBeDir = false
    )
    private val chunkedFile by option("-o", help = "output file for dumped chunks").file()
    private val quiet by option("-q", "--quiet", help = "disable warnings").flag(default = false)
    private val json = Json { prettyPrint = true }
    override fun run() {
        val sourceMap = Json.decodeFromString<SourceMapFile>(sourceMapFile.readText())
        val segments = sourceMap.buildSegments(quiet)
        chunkedFile?.writeText(json.encodeToString(segments))
    }
}

class BuildKotlinWatSegments : CliktCommand(help = "test command. Will be removed") {
    private val kotlinSegmentsFile by argument("<kotlin-segments>", help = "Processed segments for wasm map").file(
        mustBeReadable = true,
        mustExist = true,
        canBeDir = false
    )
    private val watSegmentsFile by argument("<wat-segments>", help = "Processed segments for wat map").file(
        mustExist = true,
        mustBeReadable = true,
        canBeDir = false
    )
    private val outputFile by option("-o", help = "output file").file()

    override fun run() {
        val kotlinSegments = Json.decodeFromString<List<SourceMapSegment>>(kotlinSegmentsFile.readText())
        val watSegments = Json.decodeFromString<List<SourceMapSegment>>(watSegmentsFile.readText())

        val executor = BuildMappingExecutor(kotlinSegments, watSegments)

        outputFile?.writeText(Json.encodeToString(executor.matchedSegments))
    }

}
