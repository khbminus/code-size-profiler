package org.jetbrains.kotlin.wasm.sizeprofiler.gradle

import org.jetbrains.kotlin.wasm.sizeprofiler.core.EdgeEntry
import org.jetbrains.kotlin.wasm.sizeprofiler.core.graph.VertexWithType
import org.jetbrains.kotlin.wasm.sizeprofiler.core.sourcemaps.SourceMapFile
import java.nio.file.Path

internal fun Map<String, VertexWithType>.fixDisplayName(): Map<String, VertexWithType> = this
    .mapValues { (_, v) ->
        var displayName = v.displayName ?: return@mapValues v
        while (displayName.startsWith("[ ")) {
            displayName = displayName.drop(2)
        }
        VertexWithType(
            name = v.name,
            size = v.size,
            type = v.type,
            displayName = displayName
        )
    }

internal fun Map<String, VertexWithType>.restoreClassSizes(edges: List<EdgeEntry>): Map<String, VertexWithType> {
    if (values.find { it.type != "class" } != null) {
        return this
    }
    val nextMap = toMutableMap()
    edges
        .filter { it.description == "parent class" }
        .forEach {
            nextMap[it.target] = VertexWithType(it.target, 0, "class", it.target)
        }
    return nextMap
}

internal fun SourceMapFile.fixPaths(fileLocation: Path, forKotlin: Boolean) = copy(sources = sources.map {
    fileLocation.resolve(if (forKotlin) ".." else ".").resolve(it).normalize().toString()
})
internal fun DumpProcessingTask.SourceMapDump.fixPaths(fileLocation: Path, forKotlin: Boolean) = copy(sources = sources.map {
    fileLocation.resolve(if (forKotlin) ".." else ".").resolve(it).normalize().toString()
})