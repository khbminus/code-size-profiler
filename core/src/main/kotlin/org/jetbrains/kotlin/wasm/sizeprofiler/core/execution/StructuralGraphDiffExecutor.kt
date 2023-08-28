package org.jetbrains.kotlin.wasm.sizeprofiler.core.execution

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.wasm.sizeprofiler.core.DifferenceGraph
import org.jetbrains.kotlin.wasm.sizeprofiler.core.DifferenceStatus
import org.jetbrains.kotlin.wasm.sizeprofiler.core.EdgeEntry
import org.jetbrains.kotlin.wasm.sizeprofiler.core.graph.Edge
import org.jetbrains.kotlin.wasm.sizeprofiler.core.graph.VertexWithType
import java.io.File
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.system.measureTimeMillis

class StructuralGraphDiffExecutor(private val graphLeft: GraphData, private val graphRight: GraphData) {
    val graph: DifferenceGraph

    init {
        val buildGraphTime = measureTimeMillis {
            graph = DifferenceGraph.buildCompressionGraph(
                graphLeft.edges, graphLeft.nodes.values.toList(),
                graphRight.edges, graphRight.nodes.values.toList()
            )
        }
        println("Constructing of compression graph finished in ${buildGraphTime}ms")
    }

    val metaNodes = graph.getMetaNodes()
    val metaNodesNames = buildMap {
        var counter = 1
        metaNodes.values.toSet().forEach {
            put(it, "MetaNode${counter++}")
        }
    }

    fun buildMetaNodes() {
        val time = measureTimeMillis {
            graph.build()
        }
        println("Building compression graph finished in $time ms")
    }

    fun outputGraph(outputDirectory: File, jsOutput: Boolean = false) {
        if (outputDirectory.exists() && outputDirectory.isFile) {
            error("output directory is a file")
        }
        outputDirectory.mkdirs()
        writeMetaNodes(outputDirectory, jsOutput)
        writeNodes(outputDirectory, jsOutput)
        writeEdges(outputDirectory, jsOutput)
        writeDifference(outputDirectory, jsOutput)
    }

    private fun writeMetaNodes(outputDirectory: File, jsOutput: Boolean) {
        val extension = if (jsOutput) "js" else "json"
        val metaNodesPrefix = if (jsOutput) "export const diffMetaNodesInfo = " else ""
        val metaNodeFile = outputDirectory.resolve("metanodes.${extension}")
        metaNodeFile.writeText(metaNodesPrefix +
                Json.encodeToString(
                    MetaNodeDataEntry(
                        graph.metaNodeAdjacencyList.keys.map { metaNodesNames[it]!! },
                        metaNodes.mapKeys { (k, _) -> k }.mapValues { (_, v) -> metaNodesNames[v]!! }
                    )
                )
        )
    }

    private fun writeNodes(outputDirectory: File, jsOutput: Boolean) {
        val extension = if (jsOutput) "js" else "json"
        val nodesPrefix = if (jsOutput) "export const diffDeclarationsSizes = " else ""
        val nodesFile = outputDirectory.resolve("ir-sizes.$extension")
        val nodes = buildMap {
            graph.metaNodeAdjacencyList.keys.forEach {
                val children = it.children.map { graph.inverseVertexMap[it]!! }
                val name = metaNodesNames[it]!!
                val size = children.sumOf { nodeName ->
                    (graphRight.nodes[nodeName]?.size ?: 0) - (graphLeft.nodes[nodeName]?.size ?: 0)
                }
                val type = when (it.status) {
                    DifferenceStatus.Both -> "both"
                    DifferenceStatus.FromRight -> "right"
                    DifferenceStatus.FromLeft -> "left"
                }
                put(name, VertexWithType(name, size, type))
            }
            graph.inverseVertexMap.values.forEach { v ->
                graphLeft.nodes[v]?.let { put(v, it) }
                    ?: graphRight.nodes[v]?.let { put(v, it) }
            }
        }
        nodesFile.writeText(nodesPrefix + Json.encodeToString(nodes))
    }

    private fun writeEdges(outputDirectory: File, jsOutput: Boolean) {
        val extension = if (jsOutput) "js" else "json"
        val edgesPrefix = if (jsOutput) "export const diffReachibilityInfos =" else ""
        val edgesFile = outputDirectory.resolve("dce-graph.$extension")
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
    }

    private fun writeDifference(outputDirectory: File, jsOutput: Boolean) {
        val extension = if (jsOutput) "js" else "json"
        val nodeDiffPrefix = if (jsOutput) "export const diffDeclarationsDifference = " else ""
        val nodeDiffFile = outputDirectory.resolve("node-diff.$extension")
        val difference = buildMap {
            graph.inverseVertexMap.forEach { (differenceVertex, vertex) ->
                when (differenceVertex.status) {
                    DifferenceStatus.FromLeft -> {
                        val node = graphLeft.nodes[vertex]!!
                        put(vertex, VertexWithType(vertex, node.size, node.type))
                    }

                    DifferenceStatus.FromRight -> {
                        val node = graphRight.nodes[vertex]!!
                        put(vertex, VertexWithType(vertex, node.size, node.type))
                    }

                    DifferenceStatus.Both -> {
                        val value = graphRight.nodes[vertex]!!.size - graphLeft.nodes[vertex]!!.size
                        put(vertex, VertexWithType(vertex, value, graphRight.nodes[vertex]!!.type))
                    }
                }
                Unit
            }
        }
        nodeDiffFile.writeText(nodeDiffPrefix + Json.encodeToString(difference))
    }

    class GraphData(sizePath: Path, graphPath: Path) {
        private val _nodes = Json
            .decodeFromString<Map<String, VertexWithType>>(sizePath.readText()).toMutableMap()

        init {
            _nodes.forEach { (k, v) ->
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