package org.jetbrains.kotlin.wasm.sizeprofiler.tasks

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import org.jetbrains.kotlin.wasm.sizeprofiler.core.execution.DifferenceExecutor

class Diff : CliktCommand(help = "get difference between to size files") {
    private val files by argument("<input files>").file(
        mustExist = true,
        canBeDir = false,
        mustBeReadable = true
    ).multiple()
    private val edgeFiles by option("--edge-file", help = "additional edge file to restore nested size delta.")
        .file(
            mustExist = true,
            canBeDir = false,
            mustBeReadable = true
        )
        .multiple()
    private val names by option("--name", help = "optional name for column").multiple()
    private val exclude by option("--exclude", help = "substring of sqn to exclude")

    private val onlyAdded by option("--only-added", help = "Show only added elements").flag(default = false)
    private val onlyDeleted by option("--only-deleted", help = "Show only deleted elements").flag(default = false)

    private val outputFile by option("-o", "--output").file()
    override fun run() {
        require(!onlyAdded || !onlyDeleted) { "Not more than one --only-* flags should be enabled" }
        require(edgeFiles.isEmpty() || edgeFiles.size == files.size) { "Number of edge files should be either zero or number of ir files" }

        val diff = DifferenceExecutor(
            irSizes = files,
            edgeFiles = edgeFiles,
            names = names,
            excluded = exclude,
            mode = when {
                onlyAdded -> DifferenceExecutor.Mode.OnlyAdded
                onlyDeleted -> DifferenceExecutor.Mode.OnlyDeleted
                else -> DifferenceExecutor.Mode.Both
            }
        )
        outputFile?.let {
            when (determineExtension()) {
                EXT.JSON -> diff.writeJSON(it)
                EXT.JS -> diff.writeJS(it)
                EXT.DISPLAY -> diff.writeToConsole()
                EXT.HTML -> diff.writeHTML(it)
            }
        }
    }

    private enum class EXT {
        JS, JSON, DISPLAY, HTML
    }

    private fun determineExtension(): EXT {
        val file = outputFile ?: return EXT.DISPLAY
        return when (file.extension) {
            "js" -> EXT.JS
            "json" -> EXT.JSON
            "html" -> EXT.HTML
            else -> error("Invalid file format extension")
        }
    }
}