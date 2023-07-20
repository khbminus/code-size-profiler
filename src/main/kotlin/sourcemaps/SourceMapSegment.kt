package sourcemaps

import kotlinx.serialization.Serializable

@Serializable
data class SourceMapSegment(
    val startOffsetGenerated: Int,
    val endOffsetGenerated: Int,
    val sourceFileIndex: Int,
    val sourceStartFileLine: Int,
    val sourceStartLineColumn: Int,
    val sourceEndFileLine: Int,
    val sourceEndLineColumn: Int
)