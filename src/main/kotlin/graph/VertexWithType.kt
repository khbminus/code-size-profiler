package graph

import kotlinx.serialization.Serializable

@Serializable
open class VertexWithType(val value: Int, val type: String) {
}