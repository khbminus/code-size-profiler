package dominator

import graph.DirectGraphWithSingleSource
import graph.DirectedGraph

interface GraphPreprocessor {
    fun preprocessGraph(graph: DirectedGraph): DirectGraphWithSingleSource
}