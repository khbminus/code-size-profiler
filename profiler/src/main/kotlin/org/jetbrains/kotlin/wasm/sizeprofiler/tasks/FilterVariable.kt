package org.jetbrains.kotlin.wasm.sizeprofiler.tasks

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.wasm.sizeprofiler.core.EdgeEntry
import org.jetbrains.kotlin.wasm.sizeprofiler.core.execution.FilterExecutor
import org.jetbrains.kotlin.wasm.sizeprofiler.core.graph.VertexWithType

class FilterVariable : CliktCommand(help = "Remove variables for DCE Graph without semantic changes") {
    private val extendedIrFile by argument("<path to extended ir declarations>").file(
        mustExist = true,
        canBeDir = false,
        mustBeReadable = true,
    )
    private val dceGraphFile by argument("<path to dce graph>").file(
        mustExist = true,
        canBeDir = false,
        mustBeReadable = false
    )
    private val outputFileDir by option("-o", help = "output file").file()

    override fun run() {
        val irDeclarations = Json.decodeFromString<Map<String, VertexWithType>>(extendedIrFile.readText())
        val dceEdges = Json.decodeFromString<List<EdgeEntry>>(dceGraphFile.readText())
        val filterExecutor = FilterExecutor(irDeclarations, dceEdges)
        outputFileDir?.let(filterExecutor::writeToFile)
    }
}