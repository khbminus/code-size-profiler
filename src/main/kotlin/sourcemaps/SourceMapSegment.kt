package sourcemaps

import kotlinx.serialization.Serializable

@Serializable
data class SourceMapSegment(
    val startOffsetGenerated: Int,
    val endOffsetGenerated: Int,
    val sourceFileIndex: Int,
    val startCursor: FileCursor,
    val endCursor: FileCursor,
)