package org.jetbrains.kotlin.wasm.sizeprofiler.difference

import org.jetbrains.kotlin.wasm.sizeprofiler.graph.Edge
import org.jetbrains.kotlin.wasm.sizeprofiler.graph.VertexWithType

class DifferenceGraph private constructor(
    val edges: List<DifferenceEdge>,
    private val vertexMap: Map<String, DifferenceVertex>
) {
    val adjacencyList: Map<DifferenceVertex, List<DifferenceEdge>> = buildMap {
        vertexMap.values.forEach { put(it, emptyList()) }
        putAll(edges.groupBy { it.from })
        edges.map { it.copy(from = it.to, to = it.from) }.groupBy { it.from }.forEach { (k, v) ->
            compute(k) { _, list ->
                (list ?: listOf()) + v
            }
        }
    }

    private val _metaNodeAdjacencyList: MutableMap<MetaNode, MutableSet<MetaNode>> = mutableMapOf()
    val metaNodeAdjacencyList: Map<MetaNode, MutableSet<MetaNode>>
        get() = _metaNodeAdjacencyList

    val inverseVertexMap: Map<DifferenceVertex, String> = buildMap {
        vertexMap.forEach { (k, v) -> put(v, k) }
    }

    fun getDifferenceVertex(v: VertexWithType) = vertexMap[v.name] ?: error("couldn't find any DifferenceNode for $v")

    fun build(withCoercing: Boolean = false) {
        edgeDecomposition()
        nodeDecomposition()
        buildMetaNodesAdjacencyList()
        if (withCoercing) {
            coercing()
        }
    }

    private val metaNodes: MutableMap<DifferenceVertex, MetaNode> = mutableMapOf()
    fun getMetaNodes(): Map<String, MetaNode> = metaNodes
        .filterKeys(inverseVertexMap::containsKey)
        .mapKeys { (k, _) -> inverseVertexMap[k]!! }

    private fun edgeDecomposition() {
        adjacencyList.forEach { (k, v) ->
            val edgesStatuses = v.map { it.status }.toSet()
            if (edgesStatuses.size > 1) {
                assert(k !in metaNodes)
                metaNodes[k] = MetaNode(k.status, listOf(k))
            }
        }
    }

    private fun nodeDecomposition() {
        inverseVertexMap.keys.forEach {
            if (it !in traversedInDfs && it !in metaNodes) {
                val children = mutableListOf<DifferenceVertex>()
                dfs(it, children)
                val meta = MetaNode(it.status, children)
                children.forEach {
                    assert(it !in metaNodes)
                    metaNodes[it] = meta
                }
            }
        }
    }

    private val traversedInDfs: MutableSet<DifferenceVertex> = mutableSetOf()
    private fun dfs(currentVertex: DifferenceVertex, component: MutableList<DifferenceVertex>) {
        component.add(currentVertex)
        traversedInDfs.add(currentVertex)
        adjacencyList[currentVertex]?.forEach {
            if (it.to.status == currentVertex.status && it.to !in traversedInDfs) {
                dfs(it.to, component)
            }
        }
    }

    private fun buildMetaNodesAdjacencyList() {
        val allMetaNodes = metaNodes.values.toSet()
        allMetaNodes.forEach { metaNode ->
            _metaNodeAdjacencyList[metaNode] = mutableSetOf()
            metaNode.children.forEach { child ->
                val adjust = adjacencyList[child] ?: return
                adjust.forEach { adj ->
                    val otherMetaNode = metaNodes[adj.to] ?: return
                    if (otherMetaNode != metaNode) {
                        _metaNodeAdjacencyList.compute(metaNode) { _, set ->
                            set?.also { set.add(otherMetaNode) } ?: mutableSetOf(otherMetaNode)
                        }
                    }
                }
            }
        }
    }

    private fun coercing() {
        val queue = ArrayDeque<DifferenceVertex>()
        adjacencyList.filter { (_, v) -> v.size == 1 }.forEach { (k, _) -> queue.add(k) }
        while (queue.isNotEmpty()) {
            val v = queue.removeFirst()
            for (e in adjacencyList) {
                TODO()
            }
        }
    }

    fun getAdjacencyListForVertex(v: VertexWithType): List<String>? =
        adjacencyList[vertexMap[v.name]]?.map { inverseVertexMap[it.to]!! }

    companion object {
        fun buildCompressionGraph(
            edges1: List<Edge>,
            nodes1: List<VertexWithType>,
            edges2: List<Edge>,
            nodes2: List<VertexWithType>
        ): DifferenceGraph {
            val differenceVertexes = buildMap {
                val nodes1Set = nodes1.map { it.name }.toSet()
                val nodes2Set = nodes2.map { it.name }.toSet()
                val allNodes = nodes1Set + nodes2Set
                allNodes.forEach {
                    put(it, DifferenceVertex(getStatus(it, nodes1Set, nodes2Set)))
                }
            }
            val differenceEdges = buildList {
                val edges1Set = edges1.map { it.source.name to it.target.name }.toSet()
                val edges2Set = edges2.map { it.source.name to it.target.name }.toSet()
                val allEdges = edges1Set + edges2Set
                allEdges.forEach {
                    val (source, target) = it
                    add(
                        DifferenceEdge(
                            differenceVertexes[source] ?: error("Couldn't find vertex $source in node list"),
                            differenceVertexes[target] ?: error("Couldn't find vertex $target in node list"),
                            getStatus(it, edges1Set, edges2Set)
                        )
                    )
                }
            }
            return DifferenceGraph(differenceEdges, differenceVertexes)
        }

        private fun <T> getStatus(it: T, container1: Set<T>, container2: Set<T>): DifferenceStatus {
            return when {
                it in container1 && it in container2 -> DifferenceStatus.Both
                it in container1 -> DifferenceStatus.FromLeft
                it in container2 -> DifferenceStatus.FromRight
                else -> error("Undefined bool value")
            }
        }
    }
}