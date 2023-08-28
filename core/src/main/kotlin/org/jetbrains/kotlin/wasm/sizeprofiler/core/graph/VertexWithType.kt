package org.jetbrains.kotlin.wasm.sizeprofiler.core.graph

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
open class VertexWithType(@Transient var name: String = "", val size: Int, val type: String, val displayName: String? = "") {
    override fun toString(): String {
        return name
    }
}