import graph.Edge
import graph.Vertex
import kotlin.test.assertContains
import kotlin.test.assertEquals

infix fun Vertex.to(rhs: Vertex) = Edge(this, rhs)
class Node(private val name: String, value: Int) : Vertex(value) {
    override fun equals(other: Any?): Boolean {
        if (other !is Node) return false
        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
    override fun toString(): String {
        return "Node($name)"
    }
}

 fun <T> assertExactly(expected: List<T>, actual: List<T>) {
    assertEquals(expected.size, actual.size)
    expected.forEach { assertContains(actual, it, "couldn't find $it inside $actual") }
}