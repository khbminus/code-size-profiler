import dominator.DominatorTree
import dominator.IdentityGraphPreprocessor
import graph.DirectedGraphWithFakeSource
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DominatorTreeTests {
    @Test
    fun `test from eclipse documentation`() {
        val a = Node("A", 1)
        val b = Node("B", 13)
        val c = Node("C", 27)
        val d = Node("D", 39)
        val e = Node("E", 76)
        val f = Node("F", 239)
        val g = Node("G", 1337)
        val h = Node("H", 15)

        val edges = listOf(
            a to c,
            b to c,
            c to d,
            d to f,
            f to d,
            f to h,
            c to e,
            e to g,
            g to h
        )
        val graph = DirectedGraphWithFakeSource(edges)
        val dominatorTree = DominatorTree.build(graph, IdentityGraphPreprocessor())

        val adjList = dominatorTree.adjacencyList
        val root = adjList.keys.find { it !is Node }
        assertNotNull(root) { "Could find root vertex" }

        assertContains(adjList, c)
        assertExactly(listOf(c to d, c to e, c to h), adjList[c]!!)
        assertContains(adjList, d)
        assertExactly(listOf(d to f), adjList[d]!!)
        assertExactly(listOf(e to g), adjList[e]!!)
        assertExactly(listOf(root to a, root to b, root to c), adjList[root]!!)

        dominatorTree.run {
            listOf(f, g, h, a, b).forEach {
                assertEquals(it.size, getRetainedSize(it))
            }
            assertEquals(f.size + d.size, getRetainedSize(d))
            assertEquals(e.size + g.size, getRetainedSize(e))
            assertEquals(listOf(c, d, f, e, g, h).sumOf { it.size }, getRetainedSize(c))
        }
    }

    @Test
    fun `acyclic graph`() {
        val a = Node("A", 1)
        val b = Node("B", 13)
        val c = Node("C", 27)
        val d = Node("D", 39)
        val e = Node("E", 76)
        val f = Node("F", 239)
        val g = Node("G", 1337)
        val h = Node("H", 15)
        val i = Node("I", 30)
        val j = Node("J", 566)
        val edges = listOf(
            a to b,
            a to c,
            b to d,
            c to d,
            d to e,
            e to f,
            f to g,
            e to g,
            d to i,
            i to h,
            d to j,
            j to h
        )

        val dominatorTree = DominatorTree.build(DirectedGraphWithFakeSource(edges), IdentityGraphPreprocessor())

        val adjList = dominatorTree.adjacencyList
        val root = adjList.keys.find { it !is Node }
        assertNotNull(root) { "Couldn't find any root" }
        assertContains(adjList, a)
        assertExactly(listOf(a to b, a to c, a to d), adjList[a]!!)
        assert(b !in adjList)
        assert(c !in adjList)

        assertExactly(listOf(d to h, d to e, d to i, d to j), adjList[d]!!)

        assert(h !in adjList)
        assert(i !in adjList)
        assert(j !in adjList)

        assertExactly(listOf(e to f, e to g), adjList[e]!!)

        assert(f !in adjList)
        assert(g !in adjList)

        dominatorTree.run {
            listOf(f, g, h, i, j, b, c).forEach {
                assertEquals(it.size, getRetainedSize(it))
            }
            assertEquals(listOf(e, f, g).sumOf { it.size }, getRetainedSize(e))
            assertEquals(listOf(i, j, h, e, f, g, d).sumOf { it.size }, getRetainedSize(d))
            assertEquals(listOf(a, b, c, d, e, f, g, h, i, j).sumOf { it.size }, getRetainedSize(a))
        }
    }
}