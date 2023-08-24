package org.jetbrains.kotlin.wasm.sizeprofiler.tasks

import kotlinx.serialization.Serializable

@Serializable
data class EdgeEntry(
    val source: String,
    val target: String,
    val description: String,
    val isTargetContagious: Boolean
)