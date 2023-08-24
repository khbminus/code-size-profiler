package org.jetbrains.kotlin.wasm.sizeprofiler.difference


data class DifferenceEdge(
    val from: DifferenceVertex,
    val to: DifferenceVertex,
    val status: DifferenceStatus
)