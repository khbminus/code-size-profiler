package graph

class DirectedGraphWithFakeSource(edges: List<Edge>) :
    DirectGraphWithSingleSource(listOf()) {
    override val adjacencyList: Map<VertexWithType, List<Edge>>
    override val incomingEdges: Map<VertexWithType, List<Edge>>
    override val sourceVertex: VertexWithType = object : VertexWithType(0, "fakeRoot") {
        override fun toString(): String {
            return "Fake source"
        }
    }
    init {
        val allNodes = edges.flatMap { listOf(it.source, it.target) }.toSet()
        val inDegrees = edges.groupBy { it.target }.mapValues { (_, v) -> v.size }
        val newEdges = edges + allNodes.filter { it !in inDegrees }.map { Edge(sourceVertex, it) }
        adjacencyList = newEdges.groupBy { it.source }
        incomingEdges = newEdges.groupBy { it.target }
    }
}