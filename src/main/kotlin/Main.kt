import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import difference.DifferenceGraph
import difference.DifferenceStatus
import dominator.DominatorTree
import graph.DirectedGraphWithFakeSource
import graph.Edge
import graph.Vertex
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

@Serializable
private data class EdgeEntry(
    val source: String,
    val target: String,
    val description: String,
    val isTargetContagious: Boolean
)

@Serializable
private data class NodeEntry(val size: Int, val type: String)

data class IrNode(val name: String, override val value: Int) : Vertex(value) {
    override fun toString(): String {
        return name
    }

    override fun equals(other: Any?): Boolean {
        if (other !is IrNode) return false
        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}


private val json = Json { prettyPrint = true }

class Profiler : CliktCommand() {
    override fun run() = Unit

}

class Dominators : CliktCommand(help = "Build dominator tree and get retained size") {
    private val graphDataFile by argument("<path-to/dce-graph.json>").file(
        mustExist = true,
        mustBeReadable = true,
        canBeDir = false
    )

    private val irSizeFile by argument("<path-to/ir-sizes.json>").file(
        mustExist = true,
        mustBeReadable = true,
        canBeDir = false
    )

    private val outputFile by option("-o", "--output", help = "path to output file").file()


    override fun run() {
        val edgeEntries = Json.decodeFromString<List<EdgeEntry>>(graphDataFile.readText())
        val sizes = Json.decodeFromString<Map<String, NodeEntry>>(irSizeFile.readText()).mapValues { (_, v) -> v.size }

        val nodes = sizes.mapValues { (name, size) -> IrNode(name, size) }.toMutableMap()
        val edges = edgeEntries.filter { it.source != it.target }.map {
            val source = nodes.getOrPut(it.source) { IrNode("${it.source} <EXT>", 0) }
            val target = nodes.getOrPut(it.target) { IrNode("${it.target} <EXT>", 0) }
            Edge(source, target)
        }
        val dominatorTree = DominatorTree.build(DirectedGraphWithFakeSource(edges))
        val retainedSizes = nodes.mapValues { (_, node) -> dominatorTree.getRetainedSize(node) }
        when (determineExtension()) {
            EXT.DISPLAY -> println(json.encodeToString(retainedSizes))
            EXT.JSON -> outputFile?.writeText(Json.encodeToString(retainedSizes))
            EXT.JS -> outputFile?.writeText(
                """
                | export const kotlinRetainedSize = ${Json.encodeToString(retainedSizes)}
                """.trimMargin().trim()
            )
        }
    }

    private fun determineExtension(): EXT {
        val file = outputFile ?: return EXT.DISPLAY
        return when (file.extension) {
            "js" -> EXT.JS
            "json" -> EXT.JSON
            else -> error("Invalid file format extension")
        }
    }

    private enum class EXT {
        JS, JSON, DISPLAY
    }
}

class Diff : CliktCommand(help = "get difference between to size files") {
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
            Json.decodeFromString<Map<String, NodeEntry>>(file1.readText()).mapValues { (_, v) -> v.size }
        val contentRight =
            Json.decodeFromString<Map<String, NodeEntry>>(file2.readText()).mapValues { (_, v) -> v.size }
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

class StructuredDiff : CliktCommand(help = "get difference in graph structure") {
    private val sizeFileLeft by argument("<left irNodes file>").path(
        mustBeReadable = true,
        mustExist = true,
        canBeDir = false
    )
    private val graphDataLeft by argument("<left graph data>").path(
        mustExist = true,
        mustBeReadable = true,
        canBeDir = false
    )
    private val sizeFileRight by argument("<right irNodes file>").path(
        mustBeReadable = true,
        mustExist = true,
        canBeDir = false
    )
    private val graphDataRight by argument("<right graph data>").path(
        mustExist = true,
        mustBeReadable = true,
        canBeDir = false
    )

    private val edgeOutputFile by option("-e", "--edges", help = "Output file to store graph").path()
    private val nodesOutputFile by option("-n", "--nodes", help = "Output file to store nodes").path()

    override fun run() {
        val graphLeft = GraphData(sizeFileLeft, graphDataLeft)
        val graphRight = GraphData(sizeFileRight, graphDataRight)

        val compressionGraph = DifferenceGraph.buildCompressionGraph(
            graphLeft.edges, graphLeft.nodes.values.toList(),
            graphRight.edges, graphRight.nodes.values.toList()
        )
        compressionGraph.build()
        val edges = compressionGraph.metaNodeAdjacencyList.flatMap { (k, v) ->
            v.map {
                MetaNodeEdge(
                    k.children.map { compressionGraph.inverseVertexMap[it]!! }.sortedBy { it.toString() }.toString(),
                    it.children.map { compressionGraph.inverseVertexMap[it]!! }.sortedBy { it.toString() }.toString(),
                    false,
                    ""
                )
            }
        }
        val nodes = buildMap {
            compressionGraph.metaNodeAdjacencyList.keys.forEach {
                val children = it.children.map { compressionGraph.inverseVertexMap[it]!! }
                val name = children.sortedBy { it.toString() }.toString()
                val size = children.sumOf { it.value }
                val type = when (it.status) {
                    DifferenceStatus.Both -> "both"
                    DifferenceStatus.FromRight -> "right"
                    DifferenceStatus.FromLeft -> "left"
                }
                put(name, NodeEntry(size, type))
            }
        }
        edgeOutputFile?.writeText(json.encodeToString(edges)) ?: println(json.encodeToString(edges))
        nodesOutputFile?.writeText(json.encodeToString(nodes)) ?: println(json.encodeToString(nodes))
    }

    private class GraphData(sizePath: Path, graphPath: Path) {
        private val _nodes = Json
            .decodeFromString<Map<String, NodeEntry>>(sizePath.readText())
            .mapValues { (k, v) -> IrNode(k, v.size) }.toMutableMap()
        val nodes: Map<String, IrNode>
            get() = _nodes
        val edges =
            Json.decodeFromString<List<EdgeEntry>>(graphPath.readText()).filter { it.source != it.target }.map {
                val source = _nodes.getOrPut(it.source) { IrNode("${it.source} <EXT>", 0) }
                val target = _nodes.getOrPut(it.target) { IrNode("${it.target} <EXT>", 0) }
                Edge(source, target)
            }
    }

    @Serializable
    private data class MetaNodeEdge(
        val source: String,
        val target: String,
        val isTargetContagious: Boolean,
        val description: String
    )
}


fun main(args: Array<String>) = Profiler()
    .subcommands(Dominators(), Diff(), StructuredDiff())
    .main(args)