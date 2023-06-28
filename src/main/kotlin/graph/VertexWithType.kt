package graph

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
open class VertexWithType(@Transient var name: String = "", val size: Int, val type: String) {
    override fun toString(): String {
        return name
    }
}