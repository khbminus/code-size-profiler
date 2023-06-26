package graph

abstract class DirectGraphWithSingleSource(edges: List<Edge>) : DirectedGraph(edges) {
    abstract val sourceVertex: VertexWithType
}