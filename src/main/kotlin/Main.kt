import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import difference.DifferenceGraph
import difference.DifferenceStatus
import difference.DifferenceTree
import dominator.DominatorTree
import graph.DirectedGraphWithFakeSource
import graph.Edge
import graph.Vertex
import graph.VertexWithType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.system.measureTimeMillis

@Serializable
private data class EdgeEntry(
    val source: String,
    val target: String,
    val description: String,
    val isTargetContagious: Boolean
)

@Serializable
private data class NodeEntry(val size: Int, val type: String)

data class IrNode(val name: String, override val value: Int, val type: String) : Vertex(value) {
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
    private val irSizeFile by argument("<path-to/ir-sizes.json>").file(
        mustExist = true,
        mustBeReadable = true,
        canBeDir = false
    )

    private val graphDataFile by argument("<path-to/dce-graph.json>").file(
        mustExist = true,
        mustBeReadable = true,
        canBeDir = false
    )

    private val outputFile by option("-o", "--output", help = "path to output file").file()
    private val edgesFile by option("-e", "--edges", help = "path to output file for edges").file()
    private val isParentEdgeFormat by option("--parent", help = "format of edge file").flag(
        "--graph",
        default = false,
        defaultForHelp = "use graph format"
    )


    override fun run() {
        val edgeEntries = Json.decodeFromString<List<EdgeEntry>>(graphDataFile.readText())
        val sizes = Json.decodeFromString<Map<String, NodeEntry>>(irSizeFile.readText())

        val nodes = sizes.mapValues { (name, data) -> IrNode(name, data.size, data.type) }.toMutableMap()
        val edges = edgeEntries.filter { it.source != it.target }.map {
            val source = nodes.getOrPut(it.source) { IrNode("${it.source} <EXT>", 0, "unknown") }
            val target = nodes.getOrPut(it.target) { IrNode("${it.target} <EXT>", 0, "unknown") }
            Edge(source, target)
        }
        val dominatorTree = DominatorTree.build(DirectedGraphWithFakeSource(edges))
        val retainedSizes = nodes.mapValues { (_, node) -> NodeEntry(dominatorTree.getRetainedSize(node), node.type) }
        when (outputFile.determineExtension()) {
            EXT.DISPLAY -> println(json.encodeToString(retainedSizes))
            EXT.JSON -> outputFile?.writeText(Json.encodeToString(retainedSizes))
            EXT.JS -> outputFile?.writeText(
                """
                | export const kotlinRetainedSize = ${Json.encodeToString(retainedSizes)}
                """.trimMargin().trim()
            )
        }
        if (isParentEdgeFormat) {
            val parents = dominatorTree.dominators.mapKeys { it.toString() }.mapValues { it.toString() }
            when (edgesFile.determineExtension()) {
                EXT.DISPLAY -> println(json.encodeToString(parents))
                EXT.JSON -> edgesFile?.writeText(Json.encodeToString(parents))
                EXT.JS -> edgesFile?.writeText(
                    """
                | export const retainedTreeInfo = ${Json.encodeToString(parents)}
                """.trimMargin().trim()
                )
            }
        } else {
            dominatorTree.printEdges(edgesFile)
        }
    }

    private fun DominatorTree.printEdges(file: File?) {
        val edges = adjacencyList
            .asSequence()
            .flatMap { (_, v) -> v }
            .constrainOnce()
            .filter { it.source != sourceVertex && it.target != sourceVertex }
            .map { EdgeEntry(it.source.toString(), it.target.toString(), "", false) }
            .toList()
        when (file.determineExtension()) {
            EXT.DISPLAY -> println(json.encodeToString(edges))
            EXT.JSON -> edgesFile?.writeText(Json.encodeToString(edges))
            EXT.JS -> edgesFile?.writeText(
                """
                | export const retainedReachibilityInfo = ${Json.encodeToString(edges)}
                """.trimMargin().trim()
            )
        }
    }

    private fun File?.determineExtension(): EXT {
        val file = this ?: return EXT.DISPLAY
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

    private val outputDirectory by option("-o", "--output", help = "Path to output directory").path()
    private val jsOutput by option("--js", help = "Output as JS files")
        .flag("--no-js", default = false, defaultForHelp = "not enabled")
    private val isTree by option("--tree", help = "Compare trees instead of graphs").flag(
        "--graph",
        defaultForHelp = "Compare graph",
        default = false
    )

    override fun run() {
        if (isTree) {
            compareTree()
        } else {
            compareGraph()
        }
    }

    private fun compareTree() {
        val treeLeft = DifferenceTree.RetainedTree(
            Json.decodeFromString<Map<String, VertexWithType>>(sizeFileLeft.readText()),
            Json.decodeFromString<Map<String, String>>(graphDataLeft.readText())
        )
        val treeRight = DifferenceTree.RetainedTree(
            Json.decodeFromString<Map<String, VertexWithType>>(sizeFileRight.readText()),
            Json.decodeFromString<Map<String, String>>(graphDataRight.readText())
        )
        lateinit var tree: DifferenceTree
        val time = measureTimeMillis {
            tree = DifferenceTree.build(treeLeft, treeRight)
        }
        println("Building compressing tree finished in $time ms")
        val (extension, parentsPrefix, sizesPrefix) = if (jsOutput) listOf(
            "js",
            "export const diffTreeParents = ",
            "export const diffDeclarationsSizes = "
        ) else listOf("json", "", "")
        outputDirectory?.let {
            val parentsFile = it.resolve("parents.$extension")
            parentsFile.writeText("$parentsPrefix${Json.encodeToString(tree.parents)}")

            val nodesFile = it.resolve("ir-sizes.$extension")
            nodesFile.writeText("$sizesPrefix${Json.encodeToString(tree.nodes)}")
        }
    }

    private fun compareGraph() {
        val graphLeft = GraphData(sizeFileLeft, graphDataLeft)
        val graphRight = GraphData(sizeFileRight, graphDataRight)

        val compressionGraph = DifferenceGraph.buildCompressionGraph(
            graphLeft.edges, graphLeft.nodes.values.toList(),
            graphRight.edges, graphRight.nodes.values.toList()
        )
        val time = measureTimeMillis {
            compressionGraph.build()
        }
        println("Building compression graph finished in $time ms")
        outputGraph(compressionGraph, graphLeft, graphRight)
    }

    private fun outputGraph(graph: DifferenceGraph, graphLeft: GraphData, graphRight: GraphData) {
        val dir = outputDirectory?.toFile() ?: return
        if (dir.exists() && dir.isFile) {
            error("output directory is a file")
        }
        dir.mkdirs()
        val extension = if (jsOutput) "js" else "json"
        val metaNodesPrefix = if (jsOutput) "export const diffMetaNodesInfo = " else ""
        val metaNodeFile = dir.resolve("metanodes.${extension}")
        val metaNodes = graph.getMetaNodes()
        val metaNodesNames = buildMap {
            var counter = 1
            metaNodes.values.toSet().forEach {
                put(it, "MetaNode${counter++}")
            }
        }
        metaNodeFile.writeText(metaNodesPrefix +
                Json.encodeToString(
                    MetaNodeDataEntry(
                        graph.metaNodeAdjacencyList.keys.map { metaNodesNames[it]!! },
                        metaNodes.mapKeys { (k, _) -> k.toString() }.mapValues { (_, v) -> metaNodesNames[v]!! }
                    )
                )
        )
        val nodesPrefix = if (jsOutput) "export const diffDeclarationsSizes = " else ""
        val nodesFile = dir.resolve("ir-sizes.$extension")
        val nodes = buildMap {
            graph.metaNodeAdjacencyList.keys.forEach {
                val children = it.children.map { graph.inverseVertexMap[it]!! }
                val name = metaNodesNames[it]!!
                val size = children.sumOf {
                    val name = it.toString()
                    (graphRight.nodes[name]?.value ?: 0) - (graphLeft.nodes[name]?.value ?: 0)
                }
                val type = when (it.status) {
                    DifferenceStatus.Both -> "both"
                    DifferenceStatus.FromRight -> "right"
                    DifferenceStatus.FromLeft -> "left"
                }
                put(name, NodeEntry(size, type))
            }
            graph.inverseVertexMap.values.forEach { v ->
                graphLeft.nodes[v.toString()]?.let { put(v.toString(), NodeEntry(it.value, it.type)) }
                    ?: graphRight.nodes[v.toString()]?.let { put(v.toString(), NodeEntry(it.value, it.type)) }
            }
        }
        nodesFile.writeText(nodesPrefix + Json.encodeToString(nodes))

        val edgesPrefix = if (jsOutput) "export const diffReachibilityInfos =" else ""
        val edgesFile = dir.resolve("dce-graph.$extension")
        val edges = graph.metaNodeAdjacencyList.flatMap { (k, v) ->
            v.map {
                EdgeEntry(
                    metaNodesNames[k]!!,
                    metaNodesNames[it]!!,
                    "",
                    false
                )
            }
        } + graphLeft
            .edges
            .filter { it.source.toString() in nodes && it.target.toString() in nodes }
            .map { EdgeEntry(it.source.toString(), it.target.toString(), "", false) } +
                graphRight
                    .edges
                    .filter { it.source.toString() in nodes && it.target.toString() in nodes }
                    .map { EdgeEntry(it.source.toString(), it.target.toString(), "", false) }
        edgesFile.writeText(edgesPrefix + Json.encodeToString(edges.toSet().toList()))

        val nodeDiffPrefix = if (jsOutput) "export const diffDeclarationsDifference = " else ""
        val nodeDiffFile = dir.resolve("node-diff.$extension")
        val difference = buildMap {
            graph.inverseVertexMap.forEach { (differenceVertex, vertex) ->
                val name = vertex.toString()
                when (differenceVertex.status) {
                    DifferenceStatus.FromLeft -> {
                        val node = graphLeft.nodes[name]!!
                        put(name, NodeEntry(node.value, node.type))
                    }

                    DifferenceStatus.FromRight -> {
                        val node = graphRight.nodes[name]!!
                        put(name, NodeEntry(node.value, node.type))
                    }

                    DifferenceStatus.Both -> {
                        val value = graphRight.nodes[name]!!.value - graphLeft.nodes[name]!!.value
                        put(name, NodeEntry(value, graphRight.nodes[name]!!.type))
                    }
                }
                Unit
            }
        }
        nodeDiffFile.writeText(nodeDiffPrefix + Json.encodeToString(difference))
    }

    private class GraphData(sizePath: Path, graphPath: Path) {
        private val _nodes = Json
            .decodeFromString<Map<String, NodeEntry>>(sizePath.readText())
            .mapValues { (k, v) -> IrNode(k, v.size, v.type) }.toMutableMap()
        val nodes: Map<String, IrNode>
            get() = _nodes
        val edges =
            Json.decodeFromString<List<EdgeEntry>>(graphPath.readText()).filter { it.source != it.target }.map {
                val source = _nodes.getOrPut(it.source) { IrNode(it.source, 0, "unknown") }
                val target = _nodes.getOrPut(it.target) { IrNode(it.target, 0, "unknown") }
                Edge(source, target)
            }
    }

    @Serializable
    private data class MetaNodeDataEntry(
        val metaNodesList: List<String>,
        val parent: Map<String, String>
    )
}


fun main(args: Array<String>) = Profiler()
    .subcommands(Dominators(), Diff(), StructuredDiff())
    .main(args)