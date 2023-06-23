package graph

import kotlinx.serialization.Serializable

@Serializable
open class VertexWithType(val size: Int, val type: String) {
}