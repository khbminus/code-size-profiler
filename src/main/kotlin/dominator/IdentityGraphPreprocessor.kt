package dominator

import graph.DirectGraphWithSingleSource
import graph.DirectedGraph

class IdentityGraphPreprocessor : GraphPreprocessor {
    override fun preprocessGraph(graph: DirectedGraph): DirectGraphWithSingleSource {
        require(graph is DirectGraphWithSingleSource) { "Identity preprocessor requires single source graph" }
        val postOrder = graph.getPostOrder().toSet()
        require(graph.adjacencyList.keys.all { it in postOrder }) { "All vertexes should be reachable from source" }
        require(graph.incomingEdges.getOrDefault(graph.sourceVertex, emptyList()).isEmpty()) {
            "Source vertex should has in-degree zero"
        }
        return graph
    }
}