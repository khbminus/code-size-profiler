package org.jetbrains.kotlin.wasm.sizeprofiler.core.execution

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.wasm.sizeprofiler.core.sourcemaps.SourceMapSegment

class BuildMappingExecutor(
    kotlinSegments: List<SourceMapSegment>,
    watSegments: List<SourceMapSegment>,

) {
    val matchedSegments: List<MatchingSegment>
    init {
        var currentWatSegmentIndex = 0
        matchedSegments = kotlinSegments.map { ktSegment ->
            while (currentWatSegmentIndex < watSegments.size && watSegments[currentWatSegmentIndex].startOffsetGenerated < ktSegment.startOffsetGenerated) {
                currentWatSegmentIndex++
            }
            require(currentWatSegmentIndex < watSegments.size)
            val startPosition = currentWatSegmentIndex
            while (currentWatSegmentIndex < watSegments.size && watSegments[currentWatSegmentIndex].endOffsetGenerated <= ktSegment.endOffsetGenerated) {
                currentWatSegmentIndex++
            }
            val watSegment = watSegments[startPosition].copy(
                endOffsetGenerated = watSegments[currentWatSegmentIndex - 1].startOffsetGenerated,
                endCursor = watSegments[currentWatSegmentIndex - 1].endCursor
            )
            MatchingSegment(ktSegment, watSegment)
        }
    }
    @Serializable
    data class MatchingSegment(val kotlinSegment: SourceMapSegment, val watSegment: SourceMapSegment)
}