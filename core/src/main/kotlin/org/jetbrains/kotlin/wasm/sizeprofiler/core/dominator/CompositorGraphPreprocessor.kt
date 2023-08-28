package org.jetbrains.kotlin.wasm.sizeprofiler.core.dominator

import org.jetbrains.kotlin.wasm.sizeprofiler.core.graph.DirectGraphWithSingleSource


fun GraphPreprocessor.compose(right: GraphPreprocessor) = CompositorGraphPreprocessor(this, right)
class CompositorGraphPreprocessor(private val left: GraphPreprocessor, private val right: GraphPreprocessor) :
    GraphPreprocessor {
    override fun preprocessGraph(graph: DirectGraphWithSingleSource) =
        left.preprocessGraph(right.preprocessGraph(graph))
}