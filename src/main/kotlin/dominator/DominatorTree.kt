package dominator

import graph.DirectGraphWithSingleSource
import graph.Edge
import graph.Vertex

/**
 * Computing dominators tree of a graph, using:
 * Cooper, Keith D., Timothy J. Harvey, and Ken Kennedy. A simple, fast dominance algorithm. 2006.
 *
 * Construction of the tree works in O(|V|^2), but practically faster than Lengauer-Tarjan algorithm
 */
class DominatorTree private constructor(private val dominators: Map<Vertex, Vertex>, override val sourceVertex: Vertex) :
    DirectGraphWithSingleSource(dominators.filter { (k, v) -> k != v }.map { (k, v) -> Edge(v, k) }) {
    private val retainedSizes: Map<Vertex, Int> = buildMap {
        runDfs(
            { put(it, it.value) },
            {},
            { computeIfPresent(it.source) { _, v -> v + getOrDefault(it.target, 0) } }
        )
    }

    /**
     * Get a retained size of the vertex. Retained size is the size of a vertex is
     * sum of the shallow size of all vertexes that are dominated by vertex. Works in O(1)
     */
    fun getRetainedSize(vertex: Vertex) = retainedSizes[vertex] ?: error("Couldn't find vertex $vertex")

    companion object {
        fun build(graph: DirectGraphWithSingleSource) = DominatorTreeBuilder(graph).build()
    }

    private class DominatorTreeBuilder(private val graph: DirectGraphWithSingleSource) {
        private val vertexPostOrder: List<Vertex> = graph.getPostOrder()
        private val vertexOrder: Map<Vertex, Int> = buildMap {
            vertexPostOrder.forEachIndexed { index, v ->
                put(v, index)
            }
        }

        private val immediateDominators: MutableMap<Vertex, Vertex> = mutableMapOf()


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
                    var newImmediateDominator: Vertex? = null
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

        private fun intersect(v1: Vertex, v2: Vertex): Vertex {
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

        private fun Vertex.order(): Int = vertexOrder[this] ?: error("couldn't find order for vertex $this")
    }
}