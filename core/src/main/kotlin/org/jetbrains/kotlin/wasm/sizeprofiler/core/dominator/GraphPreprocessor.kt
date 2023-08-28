package org.jetbrains.kotlin.wasm.sizeprofiler.core.dominator

import org.jetbrains.kotlin.wasm.sizeprofiler.core.graph.DirectGraphWithSingleSource


interface GraphPreprocessor {
    fun preprocessGraph(graph: DirectGraphWithSingleSource): DirectGraphWithSingleSource
}