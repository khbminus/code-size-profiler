package tasks

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import sourcemaps.SourceMapFile
import java.io.File
import java.nio.file.Path
import kotlin.io.path.readText

class SourceMaps : CliktCommand(help = "Read and process sourcemap") {
    private val sourceMapFile: Path by argument("<path to wasm sourcemap file>").path(
        mustExist = true,
        mustBeReadable = true,
        canBeDir = false
    )
    private val chunkedFile by option("-o", help = "output file for dumped chunks").file()
    private val json = Json { prettyPrint = true }
    override fun run() {
        val sourceMap = Json.decodeFromString<SourceMapFile>(sourceMapFile.readText())
        val segments = sourceMap.buildSegments()
//        val sourceMapFolder = sourceMapFile.parent
        val files = sourceMap.sources.mapIndexed { index, path ->
            val file = File(path)
            when {
                sourceMap.sourcesContent?.get(index) != null -> sourceMap.sourcesContent[index]!!.lines()
                !file.exists() || !file.canRead() -> null
                else -> file.readLines()
            }
        }
        val mappedSegments = buildMap<String, String> {
            segments.forEach {
                if (files[it.sourceFileIndex] == null) {
                    return@forEach
                }
                put(
                    "${it.startOffsetGenerated}::${it.endOffsetGenerated}",
                    files[it.sourceFileIndex]?.getChunk(
                        it.sourceStartFileLine,
                        it.sourceStartLineColumn,
                        it.sourceEndFileLine,
                        it.sourceEndLineColumn
                    ) ?: "KEK"
                )
            }
        }
        chunkedFile?.writeText(json.encodeToString(mappedSegments))
    }

    private fun List<String>.getChunk(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int): String {
        val endLineFixed = endLine.coerceAtMost(size)

        if (startLine >= size) {
            return ""
        }
        if (startLine == endLineFixed) {
            return get(startLine).substring(
                startColumn,
                endColumn
            )
        }
        val prefix = get(startLine).substring(startColumn)
        val suffix = if (endLineFixed < size) get(endLineFixed).substring(0, endColumn) else ""
        return (listOf(prefix) + subList(startLine + 1, endLineFixed) + listOf(suffix)).joinToString(separator = "\n")
    }
}