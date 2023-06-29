package dominator

import graph.DirectGraphWithSingleSource

interface GraphPreprocessor {
    fun preprocessGraph(graph: DirectGraphWithSingleSource): DirectGraphWithSingleSource
}