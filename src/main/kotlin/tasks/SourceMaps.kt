package tasks

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import sourcemaps.SourceMapFile
import sourcemaps.SourceMapSegment
import java.io.File
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

    private val kotlinSourceMapFile by argument("<kotlin-wasm-map>", help = "kotlin-wasm source map").file(
        mustBeReadable = true,
        mustExist = true,
        canBeDir = false
    )
    private val watSourceMapFile by argument("<wat-wasm-map>", help = "wat-wasm source map").file(
        mustBeReadable = true,
        mustExist = true,
        canBeDir = false
    )
    private val outputFile by option("-o", help = "output file").file()

    private val json = Json { prettyPrint = true }

    override fun run() {
        val kotlinSegments = Json.decodeFromString<List<SourceMapSegment>>(kotlinSegmentsFile.readText())
        val watSegments = Json.decodeFromString<List<SourceMapSegment>>(watSegmentsFile.readText())

        val kotlinSourceMap = Json.decodeFromString<SourceMapFile>(kotlinSourceMapFile.readText())
        val watSourceMap = Json.decodeFromString<SourceMapFile>(watSourceMapFile.readText())

        val kotlinTexts = kotlinSourceMap.sources.mapIndexed { index, path ->
            kotlinSourceMap.sourcesContent?.get(index)?.lines()
                ?: File(path).takeIf{it.isFile && it.canRead() && it.exists()}?.readLines()
        }

        var currentWatSegmentIndex = 0
        val matched = kotlinSegments.map { ktSegment ->
            if (kotlinTexts[ktSegment.sourceFileIndex] == null) {
                return@map null
            }
            while (currentWatSegmentIndex < watSegments.size && watSegments[currentWatSegmentIndex].startOffsetGenerated < ktSegment.startOffsetGenerated) {
                currentWatSegmentIndex++
            }
            require(currentWatSegmentIndex < watSegments.size)
            val startPosition = currentWatSegmentIndex
            while (currentWatSegmentIndex < watSegments.size && watSegments[currentWatSegmentIndex].endOffsetGenerated <= ktSegment.endOffsetGenerated) {
                currentWatSegmentIndex++
            }
            val watSegment = watSegments[startPosition].copy(
                endOffsetGenerated = watSegments[currentWatSegmentIndex - 1].startOffsetGenerated,
                endCursor = watSegments[currentWatSegmentIndex - 1].endCursor
            )
            MatchingSegment(ktSegment, watSegment)
        }
        outputFile?.writeText(Json.encodeToString(matched.filterNotNull()))
    }

    @Serializable
    data class MatchingSegment(val kotlinSegment: SourceMapSegment, val watSegment: SourceMapSegment)

}
