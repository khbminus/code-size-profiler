package tasks

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import graph.VertexWithType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

class Diff : CliktCommand(help = "get difference between to size files") {
    private val json = Json { prettyPrint = true }
    private val files by argument("<input files>").file(
        mustExist = true,
        canBeDir = false,
        mustBeReadable = true
    ).multiple()
    private val edgeFiles by option("--edge-file", help = "additional edge file to restore nested size delta.")
        .file(
            mustExist = true,
            canBeDir = false,
            mustBeReadable = true
        )
        .multiple()
    private val names by option("--name", help = "optional name for column").multiple()
    private val exclude by option("--exclude", help = "substring of sqn to exclude")

    private val onlyAdded by option("--only-added", help = "Show only added elements").flag(default = false)
    private val onlyDeleted by option("--only-deleted", help = "Show only deleted elements").flag(default = false)

    private val outputFile by option("-o", "--output").file()
    override fun run() {
        require(!onlyAdded || !onlyDeleted) { "Not more than one --only-* flags should be enabled" }
        require(edgeFiles.isEmpty() || edgeFiles.size == files.size) { "Number of edge files should be either zero or number of ir files" }

        val content = files.map {
            Json.decodeFromString<Map<String, VertexWithType>>(it.readText())
                .mapValues { (_, v) -> v.size }
                .filterKeys { exclude?.let { ex -> ex !in it } ?: true }
        }
        val additionalSizes = edgeFiles
            .mapIndexed { idx, it ->
                Json
                    .decodeFromString<List<EdgeEntry>>(it.readText())
                    .filter { it.description == "parent class" }
                    .groupBy({ "${it.target} (Whole class)" }, EdgeEntry::source)
                    .mapValues { (_, v) -> v.sumOf { content[idx].getOrDefault(it, 0) } }
            }

        var iterations = 1
        val outputContent = buildMap<String, Map<String, Int>> {
            put(
                if (names.isEmpty()) files[0].name else names[0], if (additionalSizes.isEmpty()) content[0] else {
                    content[0] + additionalSizes[0]
                }
            )
            for (idx in 1..files.lastIndex) {
                val previousValues = content[idx - 1]
                val currentValues = content[idx]
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
                put(if (names.size < idx) files[idx].name else names[idx], currentValues)
            }
        }
        val columns = outputContent.keys.toList()
        val rows = outputContent
            .filterKeys { it.startsWith("Δ") }
            .asSequence()
            .map { (_, v) -> v.keys }
            .reduce(Set<String>::plus)
            .filter { (!onlyAdded || it !in content[0]) && (!onlyDeleted || it !in content.last()) }
            .sortedByDescending {
                outputContent["Δ (1)"]?.get(it) ?: 0
            }
        when (determineExtension()) {
            EXT.JSON -> outputFile?.writeText(json.encodeToString(outputContent))
            EXT.JS -> outputFile?.writeText(
                """
                | export const kotlinDifferenceInfo = ${Json.encodeToString(outputContent)}
                """.trimMargin().trim()
            )

            EXT.DISPLAY -> {

                println(columns.joinToString(separator = ";", prefix = "name;"))
                rows.forEach {
                    println(outputContent.map { (_, v) -> v[it]?.toString() ?: "" }
                        .joinToString(separator = ";", prefix = "$it;"))
                }
            }

            EXT.HTML -> {
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
                    |     <title>Difference between ${content.size} files</title></head>
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
                    output.add(outputContent.map { (_, v) -> v[it]?.toString() ?: "" }
                        .joinToString(
                            prefix = "<tr><td>${it.escape()}</td>",
                            separator = "",
                            postfix = "</tr>"
                        ) { "<td>${it.escape()}</td>" })
                }
                output.add("</tbody></table></body></html>")
                outputFile?.writeText(output.toString())
            }
        }
    }

    private enum class EXT {
        JS, JSON, DISPLAY, HTML
    }

    private fun determineExtension(): EXT {
        val file = outputFile ?: return EXT.DISPLAY
        return when (file.extension) {
            "js" -> EXT.JS
            "json" -> EXT.JSON
            "html" -> EXT.HTML
            else -> error("Invalid file format extension")
        }
    }

    private fun String.escape() = replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#039;")
}