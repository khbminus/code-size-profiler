package dominator

import graph.DirectGraphWithSingleSource
import graph.Edge
import graph.VertexWithType

/**
 * Computing dominators tree of a graph, using:
 * Cooper, Keith D., Timothy J. Harvey, and Ken Kennedy. A simple, fast dominance algorithm. 2006.
 *
 * Construction of the tree works in O(|V|^2), but practically faster than Lengauer-Tarjan algorithm
 */
class DominatorTree private constructor(val dominators: Map<VertexWithType, VertexWithType>, override val sourceVertex: VertexWithType) :
    DirectGraphWithSingleSource(dominators.filter { (k, v) -> k != v }.map { (k, v) -> Edge(v, k) }) {
    private val retainedSizes: Map<VertexWithType, Int> = buildMap {
        runDfs(
            { put(it, it.size) },
            {},
            { computeIfPresent(it.source) { _, v -> v + getOrDefault(it.target, 0) } }
        )
    }

    /**
     * Get a retained size of the vertex. Retained size is the size of a vertex is
     * sum of the shallow size of all vertexes that are dominated by vertex. Works in O(1)
     */
    fun getRetainedSize(vertex: VertexWithType) = retainedSizes[vertex] ?: vertex.size

    companion object {
        fun build(graph: DirectGraphWithSingleSource) = DominatorTreeBuilder(graph).build()
    }

    private class DominatorTreeBuilder(private val graph: DirectGraphWithSingleSource) {
        private val vertexPostOrder: List<VertexWithType> = graph.getPostOrder()
        private val vertexOrder: Map<VertexWithType, Int> = buildMap {
            vertexPostOrder.forEachIndexed { index, v ->
                put(v, index)
            }
        }

        private val immediateDominators: MutableMap<VertexWithType, VertexWithType> = mutableMapOf()


        fun build(): DominatorTree {
            immediateDominators[graph.sourceVertex] = graph.sourceVertex

            var changed: Boolean
            do {
                changed = false
                for (v in vertexPostOrder.reversed()) {
                    if (v == graph.sourceVertex) {
                        continue
                    }
                    val oldImmediateDominator = immediateDominators[v]
                    var newImmediateDominator: VertexWithType? = null
                    val edges = graph.incomingEdges[v] ?: continue
                    for (edge in edges) {
                        immediateDominators[edge.source] ?: continue
                        newImmediateDominator = newImmediateDominator?.let { intersect(edge.source, it) } ?: edge.source
                    }
                    newImmediateDominator ?: error("Couldn't find any dominator for vertex $v")
                    if (newImmediateDominator != oldImmediateDominator) {
                        changed = true
                        immediateDominators[v] = newImmediateDominator
                    }
                }
            } while (changed)
            return DominatorTree(immediateDominators, graph.sourceVertex)
        }

        private fun intersect(v1: VertexWithType, v2: VertexWithType): VertexWithType {
            var vertex1 = v1
            var vertex2 = v2
            while (vertex1 != vertex2) {
                if (vertex1.order() < vertex2.order()) {
                    vertex1 = immediateDominators[vertex1] ?: error("couldn't find IDom for $vertex2")
                } else {
                    vertex2 = immediateDominators[vertex2] ?: error("couldn't find IDom for $vertex1")
                }
            }
            return vertex1
        }

        private fun VertexWithType.order(): Int = vertexOrder[this] ?: error("couldn't find order for vertex $this")
    }
}