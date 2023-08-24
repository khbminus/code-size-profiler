package org.jetbrains.kotlin.wasm.sizeprofiler.tasks

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import org.jetbrains.kotlin.wasm.sizeprofiler.graph.VertexWithType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
    private val json = Json { prettyPrint = true }

    override fun run() {
        val irDeclarations = Json.decodeFromString<Map<String, VertexWithType>>(extendedIrFile.readText())
        val dceEdges = Json.decodeFromString<List<EdgeEntry>>(dceGraphFile.readText())
        val roots = dceEdges.filter { it.description == "<ROOT>" }.map { it.source }.toSet()
        val variables = irDeclarations.filter { (k, v) -> v.isVariable() && k !in roots }

        val parentFunctionForVariable = buildMap<String, MutableList<String>> {
            dceEdges
                .asSequence()
                .filter {
                    val source = irDeclarations.getOrFail(it.source)
                    val target = irDeclarations.getOrFail(it.target)
                    !source.isClass() && !target.isClass() && !source.isVariable() && target.isVariable()
                }.forEach { edge ->
                    compute(edge.target) { _, parents ->
                        parents?.also { parents.add(edge.source) }
                            ?: mutableListOf(edge.source)
                    }
                }
        }

        require(variables.all { (k, _) -> k in parentFunctionForVariable }) {
            "Can't find parent function for ${
                variables
                    .filter { (k, _) ->  k !in parentFunctionForVariable }
                    .size
            } variables"
        }

        val newEdges = dceEdges
            .asSequence().flatMap {edge ->

                val source = irDeclarations.getOrFail(edge.source)
                val target = irDeclarations.getOrFail(edge.target)
                when {
                    source.isVariable() && !target.isVariable() -> {
                        val newSources = parentFunctionForVariable.getOrFail(edge.source)
                        newSources.map {
                            edge.copy(source = it)
                        }
                    }
                    target.isVariable() -> emptyList()
                    else -> listOf(edge)
                }
            }.toList()
        outputFileDir?.writeText(json.encodeToString(newEdges))
    }

    private fun VertexWithType.isVariable() = type == "variable" || type == "value parameter"
    private fun VertexWithType.isClass() = type == "class"
    private fun <K, V> Map<K, V>.getOrFail(k: K) = get(k) ?: error("Can't find key $k")
}