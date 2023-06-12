package graph

open class DirectedGraph(edges: List<Edge>) {

    open val adjacencyList: Map<Vertex, List<Edge>> = edges.groupBy { it.source }
    open val incomingEdges: Map<Vertex, List<Edge>> = edges.groupBy { it.target }.toMutableMap()

    private val usedInDfs: MutableSet<Vertex> = mutableSetOf()

    fun getPostOrder(): List<Vertex> {
        val result = mutableListOf<Vertex>()
        return result.also {
            runDfs({}, result::add)
        }
    }

    protected open fun runDfs(
        enterFunction: (Vertex) -> Unit,
        exitFunction: (Vertex) -> Unit,
        afterSubTreePassed: (Edge) -> Unit = {}
    ) {
        usedInDfs.clear()
        adjacencyList.keys.forEach {
            if (it !in usedInDfs)
                dfs(it, enterFunction, exitFunction, afterSubTreePassed)
        }
    }

    private fun dfs(
        v: Vertex,
        enterFunction: (Vertex) -> Unit,
        exitFunction: (Vertex) -> Unit,
        afterEdge: (Edge) -> Unit
    ) {
         enterFunction(v)
        usedInDfs.add(v)
        adjacencyList[v]?.forEach {
            if (it.target !in usedInDfs) {
                dfs(it.target, enterFunction, exitFunction, afterEdge)
            }
             afterEdge(it)
        }
        exitFunction(v)
    }
}