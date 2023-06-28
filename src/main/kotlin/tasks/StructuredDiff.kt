package tasks

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import difference.DifferenceGraph
import difference.DifferenceStatus
import difference.DifferenceTree
import graph.Edge
import graph.VertexWithType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.system.measureTimeMillis

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
        val fakeSource = mapOf("Fake source" to VertexWithType("Fake source",0, "fake source"))
        val treeLeft = DifferenceTree.RetainedTree(
            Json.decodeFromString<Map<String, VertexWithType>>(sizeFileLeft.readText()) + fakeSource,
            Json.decodeFromString<Map<String, String>>(graphDataLeft.readText())
        )
        val treeRight = DifferenceTree.RetainedTree(
            Json.decodeFromString<Map<String, VertexWithType>>(sizeFileRight.readText()) + fakeSource,
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
                    (graphRight.nodes[name]?.size ?: 0) - (graphLeft.nodes[name]?.size ?: 0)
                }
                val type = when (it.status) {
                    DifferenceStatus.Both -> "both"
                    DifferenceStatus.FromRight -> "right"
                    DifferenceStatus.FromLeft -> "left"
                }
                put(name, VertexWithType(name, size, type))
            }
            graph.inverseVertexMap.values.forEach { v ->
                graphLeft.nodes[v.toString()]?.let { put(v.toString(), it) }
                    ?: graphRight.nodes[v.toString()]?.let { put(v.toString(), it) }
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
                    "Both",
                    false
                )
            }
        } + graph.edges.map {
            EdgeEntry(
                graph.inverseVertexMap[it.from]!!,
                graph.inverseVertexMap[it.to]!!,
                it.status.toString(),
                false
            )
        }
        edgesFile.writeText(edgesPrefix + Json.encodeToString(edges.toSet().toList()))

        val nodeDiffPrefix = if (jsOutput) "export const diffDeclarationsDifference = " else ""
        val nodeDiffFile = dir.resolve("node-diff.$extension")
        val difference = buildMap {
            graph.inverseVertexMap.forEach { (differenceVertex, vertex) ->
                val name = vertex.toString()
                when (differenceVertex.status) {
                    DifferenceStatus.FromLeft -> {
                        val node = graphLeft.nodes[name]!!
                        put(name, VertexWithType(name, node.size, node.type))
                    }

                    DifferenceStatus.FromRight -> {
                        val node = graphRight.nodes[name]!!
                        put(name, VertexWithType(name, node.size, node.type))
                    }

                    DifferenceStatus.Both -> {
                        val value = graphRight.nodes[name]!!.size - graphLeft.nodes[name]!!.size
                        put(name, VertexWithType(name, value, graphRight.nodes[name]!!.type))
                    }
                }
                Unit
            }
        }
        nodeDiffFile.writeText(nodeDiffPrefix + Json.encodeToString(difference))
    }

    private class GraphData(sizePath: Path, graphPath: Path) {
        private val _nodes = Json
            .decodeFromString<Map<String, VertexWithType>>(sizePath.readText()).toMutableMap()

        init {
            _nodes.forEach {(k, v) ->
                v.name = k
            }
        }
        val nodes: Map<String, VertexWithType>
            get() = _nodes
        val edges =
            Json.decodeFromString<List<EdgeEntry>>(graphPath.readText()).filter { it.source != it.target }.map {
                val source = _nodes.getOrPut(it.source) { VertexWithType(it.source, 0, "unknown") }
                val target = _nodes.getOrPut(it.target) { VertexWithType(it.target, 0, "unknown") }
                Edge(source, target)
            }
    }

    @Serializable
    private data class MetaNodeDataEntry(
        val metaNodesList: List<String>,
        val parent: Map<String, String>
    )
}