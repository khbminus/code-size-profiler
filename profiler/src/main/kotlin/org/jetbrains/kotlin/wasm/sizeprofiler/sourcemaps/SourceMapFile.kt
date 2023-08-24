package org.jetbrains.kotlin.wasm.sizeprofiler.sourcemaps

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class SourceMapFile(
    val version: Int,
    val file: String? = null,
    val sourceRoot: String? = null,
    val sources: List<String>,
    val sourcesContent: List<String?>? = null,
    val names: List<String>,
    val mappings: CodeMapping
) {
    init {
        require(version == 3) { "Only version 3 sourcemaps are supported!" }
        require(sourcesContent == null || sourcesContent.size == sources.size) { "SourceContent neither absent or equal sized with sources" }
    }

    fun buildSegments(quiet: Boolean = false): List<SourceMapSegment> {
        val fileEntries: Array<TreeSet<FileCursor>> = Array(sources.size) { TreeSet() }
        for (line in mappings.lines) {
            line.forEach { entry ->
                val (file, fileLine, fileColumn) = when (entry) {
                    is CodeMapping.CodeMappingEntry.NoSourceEntry -> listOf(null, null, null)
                    is CodeMapping.CodeMappingEntry.NotNamedSourceEntry ->
                        listOf(entry.sourceListIndex, entry.sourceFileLine, entry.sourceFileColumn)

                    is CodeMapping.CodeMappingEntry.NamedSourceEntry ->
                        listOf(entry.sourceListIndex, entry.sourceFileLine, entry.sourceFileColumn)
                }
                if (file === null || fileLine === null || fileColumn === null)
                    return@forEach
                val fileEntry = FileCursor(fileLine, fileColumn)

                require(file < fileEntries.size) { "Entry $entry has invalid file > ${fileEntries.size}" }
                if(!quiet && fileEntry in fileEntries[file]) {
                    println("\u001b[31m Warning: Duplicate index for $file built for $entry\u001b[0m")
                }
                fileEntries[file].add(fileEntry)
            }
        }

        return buildList {
            for (line in mappings.lines) {
                val faked = line + listOf(CodeMapping.CodeMappingEntry.NoSourceEntry(Int.MAX_VALUE))
                faked.zipWithNext().forEach { (current, next) ->
                    val startOffset = current.generatedStartingColumn
                    val endOffset = next.generatedStartingColumn
                    val (file, fileLine, fileColumn) = current.getFileInfo()
                    if (file === null || fileLine === null || fileColumn === null)
                        return@forEach
                    val endFilePosition =
                        fileEntries[file].higher(FileCursor(fileLine, fileColumn))
                            ?: FileCursor(Int.MAX_VALUE, 0)
                    add(
                        SourceMapSegment(
                            startOffsetGenerated = startOffset,
                            endOffsetGenerated = endOffset,
                            sourceFileIndex = file,
                            startCursor = FileCursor(fileLine, fileColumn),
                            endCursor = FileCursor(endFilePosition.line, endFilePosition.column)
                        )
                    )
                }
            }
        }
    }

    private fun CodeMapping.CodeMappingEntry.getFileInfo(): List<Int?> = when (this) {
        is CodeMapping.CodeMappingEntry.NoSourceEntry -> listOf(null, null, null)
        is CodeMapping.CodeMappingEntry.NotNamedSourceEntry -> listOf(sourceListIndex, sourceFileLine, sourceFileColumn)
        is CodeMapping.CodeMappingEntry.NamedSourceEntry -> listOf(sourceListIndex, sourceFileLine, sourceFileColumn)
    }

}