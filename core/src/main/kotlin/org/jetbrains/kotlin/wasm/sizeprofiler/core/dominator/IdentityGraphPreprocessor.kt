package org.jetbrains.kotlin.wasm.sizeprofiler.core.dominator

import org.jetbrains.kotlin.wasm.sizeprofiler.core.graph.DirectGraphWithSingleSource


class IdentityGraphPreprocessor : GraphPreprocessor {
    override fun preprocessGraph(graph: DirectGraphWithSingleSource): DirectGraphWithSingleSource {
        val postOrder = graph.getPostOrder().toSet()
        require(graph.adjacencyList.keys.all { it in postOrder }) { "All vertexes should be reachable from source" }
        require(graph.incomingEdges.getOrDefault(graph.sourceVertex, emptyList()).isEmpty()) {
            "Source vertex should has in-degree zero"
        }
        return graph
    }
}