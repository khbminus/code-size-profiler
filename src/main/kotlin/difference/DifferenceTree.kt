package difference

import graph.VertexWithType

class DifferenceTree private constructor(val parents: Map<String, String>, val nodes: Map<String, VertexWithType>) {
    data class RetainedTree(val nodes: Map<String, VertexWithType>, val parents: Map<String, String>)
    class DifferenceTreeVertex(val status: DifferenceStatus, val delta: Int) :
        VertexWithType(delta, status.toString())

    private class DifferenceTreeBuilder(
        val nodes: Map<String, DifferenceTreeVertex>,
        val parents: Map<String, String>
    ) {
        val adjacencyList: Map<DifferenceTreeVertex, List<DifferenceTreeVertex>> = buildMap {
            putAll(parents
                .asSequence()
                .filter { (to, from) -> to != from } // root case
                .map { (to, from) -> nodes[from]!! to nodes[to]!! }
                .groupBy({ (from, _) -> from }, { (_, to) -> to })
            )
            nodes.values.forEach {
                putIfAbsent(it, listOf())
            }
        }
        val root = parents.filter { (k, v) -> k == v }.entries.toList().first().key.let { nodes[it]!! }
        val compressionStateMap = mutableMapOf<DifferenceTreeVertex, CompressionState>()

        fun build(): DifferenceTree {
            calculateCompressionState(root)
            return DifferenceTree(
                parents, nodes.mapValues { (_, v) -> VertexWithType(v.delta, compressionStateMap[v]!!.toString()) }
            )
        }

        private fun calculateCompressionState(v: DifferenceTreeVertex) {
            compressionStateMap[v] = v.calculateState()
            for (u in adjacencyList[v]!!) {
                calculateCompressionState(u)
                compressionStateMap.compute(v) { _, state ->
                    val realState = state ?: v.calculateState()
                    val childrenState = compressionStateMap[u]
                    if (childrenState == realState) realState
                    else CompressionState.Mixed
                }
            }
        }

        enum class CompressionState {
            NotChanged, Added, Removed, Mixed
        }

        private fun DifferenceTreeVertex.calculateState(): CompressionState = when {
            delta > 0 -> CompressionState.Added
            delta == 0 -> CompressionState.NotChanged
            else -> CompressionState.Removed
        }

    }

    companion object {
        fun build(treeLeft: RetainedTree, treeRight: RetainedTree): DifferenceTree {
            val treeLeftSet = treeLeft.nodes.keys
            val treeRightSet = treeRight.nodes.keys
            val newParents = mutableMapOf<String, String>()
            val allNodeMap = buildMap {
                (treeLeft.nodes + treeRight.nodes).forEach { (name, vertex) ->
                    val parentLeft = treeLeft.parents[name]
                    val parentRight = treeRight.parents[name]
                    when {
                        name !in treeLeftSet -> {
                            put(
                                name, DifferenceTreeVertex(
                                    DifferenceStatus.FromRight,
                                    vertex.value
                                )
                            )
                            newParents[name] = treeRight.parents[name]!!
                        }

                        name !in treeRightSet -> {
                            put(
                                name, DifferenceTreeVertex(
                                    DifferenceStatus.FromLeft,
                                    -vertex.value
                                )
                            )
                            newParents[name] = treeLeft.parents[name]!!
                        }

                        parentLeft == parentRight -> {
                            val fromLeft = treeLeft.nodes[name] ?: error("Couldn't look up in left node $name")
                            val fromRight = treeRight.nodes[name] ?: error("Couldn't look up in right node $name")
                            put(
                                name,
                                DifferenceTreeVertex(
                                    DifferenceStatus.Both,
                                    fromRight.value - fromLeft.value
                                )
                            )
                            newParents[name] = parentLeft!!
                        }

                        else -> {
                            val fromLeft = treeLeft.nodes[name] ?: error("Couldn't look up in left node $name")
                            val fromRight = treeRight.nodes[name] ?: error("Couldn't look up in right node $name")
                            val nameRemoved = "$name (removed)"
                            val nameAdded = "$name (added)"
                            put(
                                nameRemoved, DifferenceTreeVertex(
                                    DifferenceStatus.FromLeft,
                                    -fromLeft.value
                                )
                            )
                            put(
                                nameAdded, DifferenceTreeVertex(
                                    DifferenceStatus.FromRight,
                                    fromRight.value
                                )
                            )
                            newParents[nameAdded] = parentRight!!
                            newParents[nameRemoved] = parentLeft!!
                        }
                    }
                }
            }
            return DifferenceTreeBuilder(allNodeMap, newParents).build()
        }
    }
}