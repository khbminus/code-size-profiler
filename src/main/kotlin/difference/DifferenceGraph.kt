package difference

import graph.Edge
import graph.Vertex

class DifferenceGraph private constructor(
    edges: List<DifferenceEdge>,
    private val vertexMap: Map<Vertex, DifferenceVertex>
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

    val inverseVertexMap: Map<DifferenceVertex, Vertex> = buildMap {
        vertexMap.forEach { (k, v) -> put(v, k) }
    }

    fun getDifferenceVertex(v: Vertex) = vertexMap[v] ?: error("couldn't find any DifferenceNode for $v")

    fun build(withCoercing: Boolean = false) {
        edgeDecomposition()
        nodeDecomposition()
        buildMetaNodesAdjacencyList()
        if (withCoercing) {
            coercing()
        }
    }

    private val metaNodes: MutableMap<DifferenceVertex, MetaNode> = mutableMapOf()
    fun getMetaNodes(): Map<Vertex, MetaNode>  = metaNodes
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

    fun getAdjacencyListForVertex(v: Vertex): List<Vertex>? =
        adjacencyList[vertexMap[v]]?.map { inverseVertexMap[it.to]!! }

    companion object {
        fun buildCompressionGraph(
            edges1: List<Edge>,
            nodes1: List<Vertex>,
            edges2: List<Edge>,
            nodes2: List<Vertex>
        ): DifferenceGraph {
            val differenceVertexes = buildMap {
                val nodes1Set = nodes1.toSet()
                val nodes2Set = nodes2.toSet()
                val allNodes = (nodes1 + nodes2).toSet()
                allNodes.forEach {
                    put(it, DifferenceVertex(getStatus(it, nodes1Set, nodes2Set)))
                }
            }
            val differenceEdges = buildList {
                val allEdges = (edges1 + edges2).toSet().toList()
                val edges1Set = edges1.toSet()
                val edges2Set = edges2.toSet()
                allEdges.forEach {
                    add(
                        DifferenceEdge(
                            differenceVertexes[it.source] ?: error("Couldn't find vertex ${it.source} in node list"),
                            differenceVertexes[it.target] ?: error("Couldn't find vertex ${it.target} in node list"),
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