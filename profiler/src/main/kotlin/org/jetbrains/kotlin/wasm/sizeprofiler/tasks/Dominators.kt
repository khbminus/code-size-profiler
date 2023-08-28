package org.jetbrains.kotlin.wasm.sizeprofiler.tasks

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.wasm.sizeprofiler.core.EdgeEntry
import org.jetbrains.kotlin.wasm.sizeprofiler.core.execution.DominatorsExecutor
import org.jetbrains.kotlin.wasm.sizeprofiler.core.graph.VertexWithType
import java.io.File

class Dominators : CliktCommand(help = "Build dominator tree and get retained size") {
    private val irSizeFile by argument("<path-to/ir-sizes.json>").file(
        mustExist = true,
        mustBeReadable = true,
        canBeDir = false
    )

    private val graphDataFile by argument("<path-to/dce-graph.json>").file(
        mustExist = true,
        mustBeReadable = true,
        canBeDir = false
    )

    private val outputFile by option("-o", "--output", help = "path to output file").file()
    private val edgesFile by option("-e", "--edges", help = "path to output file for edges").file()
    private val isParentEdgeFormat by option("--tree", help = "format of edge file").flag(
        "--graph",
        default = false,
        defaultForHelp = "use graph format"
    )
    private val removeUnknown by option(
        "--remove-unknown",
        "-r",
        help = "remove nodes, that are not stated in IR sizes"
    ).flag("--leave-unknown", default = false, defaultForHelp = "leave unknown and add it to result")


    override fun run() {
        val edgeEntries = Json.decodeFromString<List<EdgeEntry>>(graphDataFile.readText())
        val sizes = Json.decodeFromString<Map<String, VertexWithType>>(irSizeFile.readText())

        val dominatorExecutor = DominatorsExecutor(
            edgeEntries = edgeEntries,
            irSizes = sizes,
            removeUnknown = removeUnknown
        )

        when (outputFile.determineExtension()) {
            EXT.DISPLAY -> dominatorExecutor.writeSizesToConsole()
            EXT.JSON -> dominatorExecutor.writeSizesJSON(outputFile!!)
            EXT.JS -> dominatorExecutor.writeSizesJS(outputFile!!)
        }
        if (isParentEdgeFormat) {
            when (edgesFile.determineExtension()) {
                EXT.DISPLAY -> dominatorExecutor.writeEdgesToConsole()
                EXT.JSON -> dominatorExecutor.writeEdgesJSON(outputFile!!)
                EXT.JS -> dominatorExecutor.writeEdgesJs(outputFile!!)
            }
        }
    }

    private fun File?.determineExtension(): EXT {
        val file = this ?: return EXT.DISPLAY
        return when (file.extension) {
            "js" -> EXT.JS
            "json" -> EXT.JSON
            else -> error("Invalid file format extension")
        }
    }

    private enum class EXT {
        JS, JSON, DISPLAY
    }
}