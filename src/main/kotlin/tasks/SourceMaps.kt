package tasks

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.serialization.json.Json
import sourcemaps.CodeMapping
import sourcemaps.SourceMapFile
import java.nio.file.Path
import kotlin.io.path.readLines
import kotlin.io.path.readText

class SourceMaps : CliktCommand(help = "Read and process sourcemap") {
    val sourceMapFile: Path by argument("<path to wasm sourcemap file>").path(
        mustExist = true,
        mustBeReadable = true,
        canBeDir = false
    )
    override fun run() {
        val sourceMap = Json.decodeFromString<SourceMapFile>(sourceMapFile.readText())
        sourceMap.buildSegments().forEach { println(it) }
    }
}