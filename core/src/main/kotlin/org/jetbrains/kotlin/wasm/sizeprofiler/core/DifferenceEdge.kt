package org.jetbrains.kotlin.wasm.sizeprofiler.core


data class DifferenceEdge(
    val from: DifferenceVertex,
    val to: DifferenceVertex,
    val status: DifferenceStatus
)