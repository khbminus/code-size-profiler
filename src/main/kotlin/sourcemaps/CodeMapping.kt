package sourcemaps

import kotlinx.serialization.Serializable

@Serializable(with = CodeMappingSerializer::class)
data class CodeMapping(val lines: List<List<CodeMappingEntry>>) {
    companion object {
        fun fromBase64VLQ(inputString: String): CodeMapping {
            val decoder = Base64VLQDecoder()
            val base64Lines = inputString.split(";")
            return CodeMapping(
                lines = base64Lines.map {
                    it.split(",").map {
                        val decoded = decoder.decode(it)
                        when (decoded.size) {
                            1 -> CodeMappingEntry.NoSourceEntry(decoded[0])
                            4 -> {
                                val (generatedStartingColumn, sourceListIndex, sourceFileLine, sourceFileColumn) = decoded
                                CodeMappingEntry.NotNamedSourceEntry(
                                    generatedStartingColumn,
                                    sourceListIndex,
                                    sourceFileLine,
                                    sourceFileColumn
                                )
                            }

                            5 -> {
                                val (generatedStartingColumn, sourceListIndex, sourceFileLine, sourceFileColumn, sourceName) = decoded
                                CodeMappingEntry.NamedSourceEntry(
                                    generatedStartingColumn,
                                    sourceListIndex,
                                    sourceFileLine,
                                    sourceFileColumn,
                                    sourceName
                                )
                            }

                            else -> error("invalid entry $it: a result array size should be 1, 4 or 5")
                        }
                    }
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