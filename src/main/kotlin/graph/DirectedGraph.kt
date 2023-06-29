package graph

open class DirectedGraph(val edges: List<Edge>) {

    open val adjacencyList: Map<VertexWithType, List<Edge>> = edges.groupBy { it.source }
    open val incomingEdges: Map<VertexWithType, List<Edge>> = edges.groupBy { it.target }.toMutableMap()

    protected val usedInDfs: MutableSet<VertexWithType> = mutableSetOf()

    fun getPostOrder(): List<VertexWithType> {
        val result = mutableListOf<VertexWithType>()
        return result.also {
            runDfs({}, exitFunction = result::add)
        }
    }

    open fun runDfs(
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

    protected fun dfs(
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