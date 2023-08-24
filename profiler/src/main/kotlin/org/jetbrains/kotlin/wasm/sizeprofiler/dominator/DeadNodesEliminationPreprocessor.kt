package org.jetbrains.kotlin.wasm.sizeprofiler.dominator

import org.jetbrains.kotlin.wasm.sizeprofiler.graph.DirectGraphWithSingleSource
import org.jetbrains.kotlin.wasm.sizeprofiler.graph.VertexWithType

class DeadNodesEliminationPreprocessor : GraphPreprocessor {
    override fun preprocessGraph(graph: DirectGraphWithSingleSource): DirectGraphWithSingleSource {
        val postOrder = graph.getPostOrder().toSet()
        return object :
            DirectGraphWithSingleSource(graph.edges.filter { it.source in postOrder && it.target in postOrder }) {
            override val sourceVertex: VertexWithType = graph.sourceVertex
        }

    }
}