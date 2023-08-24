package org.jetbrains.kotlin.wasm.sizeprofiler.dominator

import org.jetbrains.kotlin.wasm.sizeprofiler.graph.DirectGraphWithSingleSource

interface GraphPreprocessor {
    fun preprocessGraph(graph: DirectGraphWithSingleSource): DirectGraphWithSingleSource
}