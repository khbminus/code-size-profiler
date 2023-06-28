package tasks

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
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
    private val exclude by option("--exclude", help = "substring of sqn to exclude")

    private val outputFile by option("-o", "--output").file()
    override fun run() {
        val content = files.map {
            Json.decodeFromString<Map<String, VertexWithType>>(it.readText())
                .mapValues { (_, v) -> v.size }
                .filterKeys { exclude?.let { ex -> ex !in it } ?: true }
        }
        var iterations = 1
        val outputContent = buildMap<String, Map<String, Int>> {
            put(files[0].name, content[0])
            files.zip(content).zipWithNext().forEach { (a, b) ->
                val (_, previousValues) = a
                val (fileName, currentValues) = b
                put("Δ (${iterations++})", buildMap {
                    (currentValues.keys + previousValues.keys).forEach {
                        put(it, currentValues.getOrDefault(it, 0) - previousValues.getOrDefault(it, 0))
                    }
                }.filterValues { it != 0 })
                put(fileName.name, currentValues)
            }
        }
        val columns = outputContent.keys.toList()
        val rows = outputContent
            .filterKeys { it.startsWith("Δ") }
            .map { (_, v) -> v.keys }
            .reduce(Set<String>::plus)
            .toList().sortedBy {
                content[1].getOrDefault(it, 0)
            }.reversed()
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

    private fun String.padCenter(columnSize: Int): String {
        require(columnSize >= length) { "column size should be at least string length" }
        val left = (columnSize - length) / 2
        val right = (columnSize - length + 1) / 2
        return "${" ".repeat(left)}$this${" ".repeat(right)}"
    }

    private fun String.escape() = replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#039;")
}