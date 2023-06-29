package graph

class DirectedGraphWithMergedRoots private constructor(
    roots: List<VertexWithType>,
    override val sourceVertex: VertexWithType,
    edges: List<Edge>
) : DirectGraphWithSingleSource(edges + roots.map { Edge(sourceVertex, it) }) {
    companion object {
        fun build(edges: List<Edge>, roots: List<VertexWithType>): DirectedGraphWithMergedRoots {
            val root = VertexWithType("Fake source", 0, "fake source")
            return DirectedGraphWithMergedRoots(roots, root, edges)
        }
    }
}