package graph

open class Edge(val source: Vertex, val target: Vertex) {
    override fun toString(): String {
        return "graph.Edge($source -> $target)"
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Edge) {
            return false
        }
        return source == other.source && target == other.target
    }

    override fun hashCode(): Int {
        var result = source.hashCode()
        result = 31 * result + target.hashCode()
        return result
    }
}