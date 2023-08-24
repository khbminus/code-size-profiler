import org.jetbrains.kotlin.wasm.sizeprofiler.difference.DifferenceGraph
import org.jetbrains.kotlin.wasm.sizeprofiler.difference.DifferenceStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CompressionTests {
    @Test
    fun `test from paper`() {
        val nodeMap = buildMap {
            ('A'..'N').forEach {
                put(it, Node(it.toString(), 0))
            }
        }
        val nodes1 = listOf(
            nodeMap['A']!!,
            nodeMap['B']!!,
            nodeMap['C']!!,
            nodeMap['E']!!,
            nodeMap['F']!!,
            nodeMap['G']!!,
            nodeMap['N']!!,
            nodeMap['K']!!,
            nodeMap['L']!!,
            nodeMap['M']!!
        )

        val nodes2 = listOf(
            nodeMap['D']!!,
            nodeMap['E']!!,
            nodeMap['F']!!,
            nodeMap['G']!!,
            nodeMap['I']!!,
            nodeMap['J']!!,
            nodeMap['H']!!,
            nodeMap['N']!!,
            nodeMap['K']!!,
            nodeMap['L']!!,
            nodeMap['M']!!
        )
        val commonEdges = listOf(
            nodeMap['E']!! to nodeMap['F']!!,
            nodeMap['E']!! to nodeMap['G']!!,
            nodeMap['K']!! to nodeMap['L']!!,
            nodeMap['L']!! to nodeMap['M']!!,
            nodeMap['M']!! to nodeMap['K']!!
        )

        val edges1 = listOf(
            nodeMap['A']!! to nodeMap['E']!!,
            nodeMap['B']!! to nodeMap['E']!!,
            nodeMap['B']!! to nodeMap['C']!!,
            nodeMap['C']!! to nodeMap['E']!!
        ) + commonEdges

        val edges2 = listOf(
            nodeMap['D']!! to nodeMap['E']!!,
            nodeMap['D']!! to nodeMap['G']!!,
            nodeMap['G']!! to nodeMap['H']!!,
            nodeMap['H']!! to nodeMap['F']!!,
            nodeMap['H']!! to nodeMap['J']!!,
            nodeMap['H']!! to nodeMap['I']!!,
            nodeMap['I']!! to nodeMap['J']!!,
            nodeMap['I']!! to nodeMap['N']!!,
            nodeMap['N']!! to nodeMap['H']!!,
        ) + commonEdges

        val compressionGraph = DifferenceGraph.buildCompressionGraph(edges1, nodes1, edges2, nodes2)
        compressionGraph.build(withCoercing = false)

        val metaNodes = compressionGraph.metaNodeAdjacencyList.keys
        val diffMap = nodeMap.mapValues { (_, v) -> compressionGraph.getDifferenceVertex(v) }

        val aMetaNode = metaNodes.find { diffMap['A']!! in it.children } ?: error("couldn't find metaNode for A")
        assertEquals(DifferenceStatus.FromLeft, aMetaNode.status)
        assertExactly(listOf(diffMap['A']!!), aMetaNode.children)

        val bcMetaNode = metaNodes.find { diffMap['B']!! in it.children } ?: error("couldn't find metaNode for B")
        assertEquals(DifferenceStatus.FromLeft, bcMetaNode.status)
        assertExactly(listOf(diffMap['B']!!, diffMap['C']!!), bcMetaNode.children)

        val eMetaNode = metaNodes.find { diffMap['E']!! in it.children } ?: error("couldn't find metaNode for E")
        val fMetaNode = metaNodes.find { diffMap['F']!! in it.children } ?: error("couldn't find metaNode for F")
        val gMetaNode = metaNodes.find { diffMap['G']!! in it.children } ?: error("couldn't find metaNode for G")
        val nMetaNode = metaNodes.find { diffMap['N']!! in it.children } ?: error("couldn't find metaNode for N")
        assertEquals(DifferenceStatus.Both, nMetaNode.status)
        assertExactly(listOf(diffMap['N']!!), nMetaNode.children)

        val dMetaNode = metaNodes.find { diffMap['D']!! in it.children } ?: error("couldn't find metaBode for D")
        assertEquals(DifferenceStatus.FromRight, dMetaNode.status)
        assertExactly(listOf(diffMap['D']!!), dMetaNode.children)

        val hijMetaNode = metaNodes.find { diffMap['H']!! in it.children } ?: error("couldn't find metaBode for H")
        assertEquals(DifferenceStatus.FromRight, hijMetaNode.status)
        assertExactly(listOf(diffMap['H']!!, diffMap['I']!!, diffMap['J']!!), hijMetaNode.children)

        val klmMetaNode = metaNodes.find { diffMap['K']!! in it.children } ?: error("couldn't find metaBode for K")
        assertEquals(DifferenceStatus.Both, klmMetaNode.status)
        assertExactly(listOf(diffMap['K']!!, diffMap['L']!!, diffMap['M']!!), klmMetaNode.children)

        assertExactly(listOf(eMetaNode), compressionGraph.metaNodeAdjacencyList[aMetaNode]!!.toList())
        assertExactly(listOf(eMetaNode), compressionGraph.metaNodeAdjacencyList[bcMetaNode]!!.toList())
        assertExactly(
            listOf(aMetaNode, bcMetaNode, fMetaNode, gMetaNode, dMetaNode),
            compressionGraph.metaNodeAdjacencyList[eMetaNode]!!.toList()
        )
        assertExactly(listOf(eMetaNode, gMetaNode), compressionGraph.metaNodeAdjacencyList[dMetaNode]!!.toList())
        assertExactly(listOf(eMetaNode, hijMetaNode), compressionGraph.metaNodeAdjacencyList[fMetaNode]!!.toList())
        assertExactly(
            listOf(eMetaNode, dMetaNode, hijMetaNode),
            compressionGraph.metaNodeAdjacencyList[gMetaNode]!!.toList()
        )
        assertExactly(listOf(hijMetaNode), compressionGraph.metaNodeAdjacencyList[nMetaNode]!!.toList())
        assertExactly(
            listOf(fMetaNode, gMetaNode, nMetaNode),
            compressionGraph.metaNodeAdjacencyList[hijMetaNode]!!.toList()
        )
    }
    @Test
    fun `same nodes different values`() {
        val node1 = Node("A", 1)
        val node2 = Node("A", 2)
        val node3 = Node("B", 1)
        val graph = DifferenceGraph.buildCompressionGraph(listOf(node1 to node3), listOf(node1, node3), emptyList(), listOf(node2))
        graph.build()
        val metaNodes = graph.metaNodeAdjacencyList.keys
        assertExactly(listOf(graph.getDifferenceVertex(node1)), metaNodes.find { graph.getDifferenceVertex(node1) in it.children }!!.children)
        assertExactly(listOf(graph.getDifferenceVertex(node3)), metaNodes.find { graph.getDifferenceVertex(node3) in it.children }!!.children)

    }
}