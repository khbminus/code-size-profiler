package org.jetbrains.kotlin.wasm.sizeprofiler.core.sourcemaps

import kotlinx.serialization.Serializable

@Serializable(with = CodeMappingSerializer::class)
data class CodeMapping(val lines: List<List<CodeMappingEntry>>) {
    companion object {
        fun fromBase64VLQ(inputString: String): CodeMapping {
            val decoder = Base64VLQDecoder()
            val base64Lines = inputString.split(";")
            return CodeMapping(
                lines = base64Lines.map {
                    var accumulatedCodeColumn = 0
                    var accumulatedSourceFileIndex = 0
                    var accumulatedFileLine = 0
                    var accumulatedFileColumn = 0
                    var accumulatedName = 0
                    it.split(",").map {
                        val decoded = decoder.decode(it)
                        when (decoded.size) {
                            1 -> {
                                accumulatedCodeColumn += decoded[0]
                                CodeMappingEntry.NoSourceEntry(accumulatedCodeColumn)
                            }

                            4 -> {
                                val (generatedStartingColumn, sourceListIndex, sourceFileLine, sourceFileColumn) = decoded

                                accumulatedCodeColumn += generatedStartingColumn
                                accumulatedSourceFileIndex += sourceListIndex
                                accumulatedFileLine += sourceFileLine
                                accumulatedFileColumn += sourceFileColumn

                                CodeMappingEntry.NotNamedSourceEntry(
                                    accumulatedCodeColumn,
                                    accumulatedSourceFileIndex,
                                    accumulatedFileLine,
                                    accumulatedFileColumn
                                )
                            }

                            5 -> {
                                val (generatedStartingColumn, sourceListIndex, sourceFileLine, sourceFileColumn, sourceName) = decoded

                                accumulatedCodeColumn += generatedStartingColumn
                                accumulatedSourceFileIndex += sourceListIndex
                                accumulatedFileLine += sourceFileLine
                                accumulatedFileColumn += sourceFileColumn
                                accumulatedName += sourceName

                                CodeMappingEntry.NamedSourceEntry(
                                    accumulatedCodeColumn,
                                    accumulatedSourceFileIndex,
                                    accumulatedFileLine,
                                    accumulatedFileColumn,
                                    accumulatedName
                                )
                            }

                            else -> error("invalid entry $it: a result array size should be 1, 4 or 5")
                        }
                    }
                        .sortedBy { it.generatedStartingColumn }
                }
            )
        }
    }

    sealed class CodeMappingEntry {
        abstract val generatedStartingColumn: Int

        data class NoSourceEntry(override val generatedStartingColumn: Int) : CodeMappingEntry()
        data class NotNamedSourceEntry(
            override val generatedStartingColumn: Int,
            val sourceListIndex: Int,
            val sourceFileLine: Int,
            val sourceFileColumn: Int,
        ) : CodeMappingEntry()

        data class NamedSourceEntry(
            override val generatedStartingColumn: Int,
            val sourceListIndex: Int,
            val sourceFileLine: Int,
            val sourceFileColumn: Int,
            val sourceName: Int
        ) : CodeMappingEntry()
    }
}