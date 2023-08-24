import org.jetbrains.kotlin.wasm.sizeprofiler.graph.Edge
import org.jetbrains.kotlin.wasm.sizeprofiler.graph.VertexWithType
import kotlin.test.assertContains
import kotlin.test.assertEquals

infix fun VertexWithType.to(rhs: VertexWithType) = Edge(this, rhs)
class Node(name: String, value: Int) : VertexWithType(name, value, name) {
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