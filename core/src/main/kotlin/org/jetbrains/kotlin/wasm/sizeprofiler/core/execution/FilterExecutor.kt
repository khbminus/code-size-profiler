package org.jetbrains.kotlin.wasm.sizeprofiler.core.execution

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.wasm.sizeprofiler.core.EdgeEntry
import org.jetbrains.kotlin.wasm.sizeprofiler.core.graph.VertexWithType
import java.io.File

class FilterExecutor(
    extendedIrSizes: Map<String, VertexWithType>,
    edges: List<EdgeEntry>
) {
    val newEdges: List<EdgeEntry>
    init {
        val roots = edges.filter { it.description == "<ROOT>" }.map { it.source }.toSet()
        val variables = extendedIrSizes.filter { (k, v) -> v.isVariable() && k !in roots }

        val parentFunctionForVariable = buildMap<String, MutableList<String>> {
            edges
                .asSequence()
                .filter {
                    val source = extendedIrSizes.getOrFail(it.source)
                    val target = extendedIrSizes.getOrFail(it.target)
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
        newEdges = edges
            .asSequence().flatMap {edge ->

                val source = extendedIrSizes.getOrFail(edge.source)
                val target = extendedIrSizes.getOrFail(edge.target)
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
    }

    val json = Json { prettyPrint = true }

    fun writeToFile(file: File) {
        file.writeText(json.encodeToString(newEdges))

    }
    private fun VertexWithType.isVariable() = type == "variable" || type == "value parameter"
    private fun VertexWithType.isClass() = type == "class"
    private fun <K, V> Map<K, V>.getOrFail(k: K) = get(k) ?: error("Can't find key $k")
}