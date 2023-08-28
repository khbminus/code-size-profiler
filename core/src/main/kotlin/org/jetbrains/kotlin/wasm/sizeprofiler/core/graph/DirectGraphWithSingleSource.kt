package org.jetbrains.kotlin.wasm.sizeprofiler.core.graph

abstract class DirectGraphWithSingleSource(edges: List<Edge>) : DirectedGraph(edges) {
    abstract val sourceVertex: VertexWithType

    override fun runDfs(
        enterFunction: (VertexWithType) -> Unit,
        exitFunction: (VertexWithType) -> Unit,
        afterSubTreePassed: (Edge) -> Unit
    ) {
        usedInDfs.clear()
        dfs(sourceVertex, enterFunction, exitFunction, afterSubTreePassed)
    }
}