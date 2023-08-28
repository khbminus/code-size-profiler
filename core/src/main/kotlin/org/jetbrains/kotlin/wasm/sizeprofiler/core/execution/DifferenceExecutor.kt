package org.jetbrains.kotlin.wasm.sizeprofiler.core.execution

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.wasm.sizeprofiler.core.EdgeEntry
import org.jetbrains.kotlin.wasm.sizeprofiler.core.graph.VertexWithType
import java.io.File
import java.util.*

class DifferenceExecutor(
    private val irSizes: List<File>,
    private val edgeFiles: List<File>,
    private val names: List<String>,
    private val excluded: String?,
    private val mode: Mode
) {
    enum class Mode {
        OnlyAdded, OnlyDeleted, Both
    }

    private val difference: Map<String, Map<String, Int>>
    private val readIrSizes: List<Map<String, Int>> = irSizes.map {
        Json.decodeFromString<Map<String, VertexWithType>>(it.readText())
            .mapValues { (_, v) -> v.size }
            .filterKeys { excluded?.let { ex -> ex !in it } ?: true }
    }

    init {
        val additionalSizes = edgeFiles
            .mapIndexed { idx, it ->
                Json
                    .decodeFromString<List<EdgeEntry>>(it.readText())
                    .filter { it.description == "parent class" }
                    .groupBy({ "${it.target} (Whole class)" }, EdgeEntry::source)
                    .mapValues { (_, v) -> v.sumOf { readIrSizes[idx].getOrDefault(it, 0) } }
            }

        var iterations = 1
        difference = buildMap<String, Map<String, Int>> {
            put(
                if (names.isEmpty()) irSizes[0].name else names[0], if (additionalSizes.isEmpty()) readIrSizes[0] else {
                    readIrSizes[0] + additionalSizes[0]
                }
            )
            for (idx in 1..irSizes.lastIndex) {
                val previousValues = readIrSizes[idx - 1]
                val currentValues = readIrSizes[idx]
                val difference = buildMap {
                    (currentValues.keys + previousValues.keys).forEach {
                        put(it, currentValues.getOrDefault(it, 0) - previousValues.getOrDefault(it, 0))
                    }
                }
                val nestedDifference = if (edgeFiles.isEmpty()) emptyMap() else buildMap {
                    val currentAdditionalValues = additionalSizes[idx]
                    val previousAdditionalValues = additionalSizes[idx - 1]
                    (currentAdditionalValues.keys + previousAdditionalValues.keys).forEach {
                        put(
                            it,
                            currentAdditionalValues.getOrDefault(it, 0) - previousAdditionalValues.getOrDefault(it, 0)
                        )
                    }
                }
                put("Δ (${iterations++})", (difference + nestedDifference).filterValues { it != 0 })
                put(if (names.size < idx) irSizes[idx].name else names[idx], currentValues)
            }
        }
    }

    private val json = Json { prettyPrint = true }

    fun writeJSON(outputFile: File) {
        outputFile.writeText(json.encodeToString(difference))
    }

    fun writeJS(outputFile: File) {
        outputFile.writeText(
            """
                    | export const kotlinDifferenceInfo = ${Json.encodeToString(difference)}
                    """.trimMargin().trim()
        )
    }

    private val rows: List<String>
        get() = difference
            .filterKeys { it.startsWith("Δ") }
            .asSequence()
            .map { (_, v) -> v.keys }
            .reduce(Set<String>::plus)
            .filter { (mode != Mode.OnlyAdded || it !in readIrSizes[0]) && (mode != Mode.OnlyDeleted || it !in readIrSizes.last()) }
            .sortedByDescending {
                difference["Δ (1)"]?.get(it) ?: 0
            }

    private val columns: List<String>
        get() = difference.keys.toList()


    fun writeHTML(outputFile: File) {
        val prefix = """
                    | <!DOCTYPE html>
                    | <html lang="en">
                    | <head>
                    | <style>
                    | table, th, td {
                    | border: 1px solid black;
                    | border-collapse: collapse;
                    | }
                    | td {
                    | text-align: center;
                    | }
                    | </style>
                    |     <meta charset="UTF-8">
                    |     <title>Difference between ${irSizes.size} files</title></head>
                    | <body>
                    | <table>
                    | <thead>
                """.trimMargin()
        val output = StringJoiner("\n")
        output.add(prefix)
        output.add(
            columns.joinToString(
                separator = "\n",
                prefix = "<th>Name</th>\n"
            ) { "<th>${it.escape()}</th>" })
        output.add("</thead><tbody>")
        rows.forEach {
            output.add(difference.map { (_, v) -> v[it]?.toString() ?: "" }
                .joinToString(
                    prefix = "<tr><td>${it.escape()}</td>",
                    separator = "",
                    postfix = "</tr>"
                ) { "<td>${it.escape()}</td>" })
        }
        output.add("</tbody></table></body></html>")
        outputFile.writeText(output.toString())
    }

    fun writeToConsole() {
        println(columns.joinToString(separator = ";", prefix = "name;"))
        rows.forEach {
            println(difference.map { (_, v) -> v[it]?.toString() ?: "" }
                .joinToString(separator = ";", prefix = "$it;"))
        }
    }

    private fun String.escape() = replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#039;")
}