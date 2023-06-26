package tasks

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import graph.VertexWithType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Diff : CliktCommand(help = "get difference between to size files") {
    private val json = Json { prettyPrint = true }
    private val file1 by argument("<1st input file>").file(
        mustExist = true,
        canBeDir = false,
        mustBeReadable = true
    )
    private val file2 by argument("<2nd input file>").file(
        mustExist = true,
        canBeDir = false,
        mustBeReadable = true
    )

    private val outputFile by option("-o", "--output").file()
    override fun run() {
        val contentLeft = // TODO: check node type
            Json.decodeFromString<Map<String, VertexWithType>>(file1.readText()).mapValues { (_, v) -> v.size }
        val contentRight =
            Json.decodeFromString<Map<String, VertexWithType>>(file2.readText()).mapValues { (_, v) -> v.size }
        val deltaContent = buildMap {
            (contentLeft.keys + contentRight.keys).forEach {
                put(it, contentRight.getOrDefault(it, 0) - contentLeft.getOrDefault(it, 0))
            }
        }.filter { (_, v) -> v != 0 }
        when (determineExtension()) {
            EXT.DISPLAY -> deltaContent.printTable("IR Element", "Size in IR instructions")
            EXT.JSON -> outputFile?.writeText(json.encodeToString(deltaContent))
            EXT.JS -> outputFile?.writeText(
                """
                | export const kotlinDifferenceInfo = ${Json.encodeToString(deltaContent)}
                """.trimMargin().trim()
            )

            EXT.HTML -> {
                outputFile?.writeText(
                    deltaContent
                        .entries
                        .sortedBy { (_, v) -> -v }
                        .joinToString(
                            prefix = """
                        <!DOCTYPE html><html lang="en"><head><meta charset="UTF-8"><title>Difference between ${file1.name} and ${file2.name}</title></head><body><table><thead><tr><th>Ir Element</th><th>Size in IR instruction, &#916;</th></tr></thead><tbody>
                    """.trimIndent(), postfix = "</tbody></table></body></html>", separator = ""
                        ) { (k, v) ->
                            "<tr><td>${k.escape()}</td><td>${v.toString().escape()}</td></tr>"
                        }
                )
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

    private fun <A, B> Map<A, B>.printTable(keyName: String, valueName: String) {
        val values = mapValues { (_, it) -> it.toString() }.mapKeys { (it, _) -> it.toString() }
        val keyColumnSize = 2 + keyName.length.coerceAtLeast(values.maxOf { (k, _) -> k.length })
        val valueColumnSize = 2 + valueName.length.coerceAtLeast(values.maxOf { (_, v) -> v.length })
        println("|${keyName.padCenter(keyColumnSize)}|${valueName.padCenter(valueColumnSize)}|")
        println("+${"-".repeat(keyColumnSize)}+${"-".repeat(valueColumnSize)}+")
        values.forEach { (k, v) ->
            println("|${k.padCenter(keyColumnSize)}|${v.padCenter(valueColumnSize)}|")
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