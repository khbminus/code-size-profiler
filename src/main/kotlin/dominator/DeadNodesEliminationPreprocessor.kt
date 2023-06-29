package dominator

import graph.DirectGraphWithSingleSource
import graph.VertexWithType

class DeadNodesEliminationPreprocessor : GraphPreprocessor {
    override fun preprocessGraph(graph: DirectGraphWithSingleSource): DirectGraphWithSingleSource {
        val postOrder = graph.getPostOrder().toSet()
        return object :
            DirectGraphWithSingleSource(graph.edges.filter { it.source in postOrder && it.target in postOrder }) {
            override val sourceVertex: VertexWithType = graph.sourceVertex
        }

    }
}