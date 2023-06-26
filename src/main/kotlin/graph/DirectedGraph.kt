package graph

open class DirectedGraph(edges: List<Edge>) {

    open val adjacencyList: Map<VertexWithType, List<Edge>> = edges.groupBy { it.source }
    open val incomingEdges: Map<VertexWithType, List<Edge>> = edges.groupBy { it.target }.toMutableMap()

    private val usedInDfs: MutableSet<VertexWithType> = mutableSetOf()

    fun getPostOrder(): List<VertexWithType> {
        val result = mutableListOf<VertexWithType>()
        return result.also {
            runDfs({}, result::add)
        }
    }

    protected open fun runDfs(
        enterFunction: (VertexWithType) -> Unit,
        exitFunction: (VertexWithType) -> Unit,
        afterSubTreePassed: (Edge) -> Unit = {}
    ) {
        usedInDfs.clear()
        adjacencyList.keys.forEach {
            if (it !in usedInDfs)
                dfs(it, enterFunction, exitFunction, afterSubTreePassed)
        }
    }

    private fun dfs(
        v: VertexWithType,
        enterFunction: (VertexWithType) -> Unit,
        exitFunction: (VertexWithType) -> Unit,
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