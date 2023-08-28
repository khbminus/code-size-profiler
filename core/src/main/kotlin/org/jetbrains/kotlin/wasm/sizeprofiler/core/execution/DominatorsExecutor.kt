package org.jetbrains.kotlin.wasm.sizeprofiler.core.execution

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.wasm.sizeprofiler.core.EdgeEntry
import org.jetbrains.kotlin.wasm.sizeprofiler.core.dominator.DeadNodesEliminationPreprocessor
import org.jetbrains.kotlin.wasm.sizeprofiler.core.dominator.DominatorTree
import org.jetbrains.kotlin.wasm.sizeprofiler.core.dominator.IdentityGraphPreprocessor
import org.jetbrains.kotlin.wasm.sizeprofiler.core.graph.DirectedGraphWithMergedRoots
import org.jetbrains.kotlin.wasm.sizeprofiler.core.graph.Edge
import org.jetbrains.kotlin.wasm.sizeprofiler.core.graph.VertexWithType
import java.io.File

class DominatorsExecutor(
    edgeEntries: List<EdgeEntry>,
    irSizes: Map<String, VertexWithType>,
    removeUnknown: Boolean
) {
    val dominatorTree: DominatorTree
    val retainedSizes: Map<String, VertexWithType>
    private val json = Json { prettyPrint = true }

    init {
        irSizes.forEach { (k, v) -> v.name = k }

        val nodes = irSizes.toMutableMap()
        val edges = edgeEntries.filter { it.source != it.target }
            .filter { !removeUnknown || (it.source in nodes && it.target in nodes) }
            .map {
                val source = nodes.getOrPut(it.source) { VertexWithType(it.source, 0, "unknown") }
                val target = nodes.getOrPut(it.target) { VertexWithType(it.target, 0, "unknown") }
                Edge(source, target)
            }
        val roots = edgeEntries.filter { it.source == it.target }
            .map { nodes.getOrPut(it.source) { VertexWithType(it.source, 0, "unknown") } }
        val newGraph = DeadNodesEliminationPreprocessor()
            .preprocessGraph(DirectedGraphWithMergedRoots.build(edges, roots))
        nodes.clear()
        nodes.putAll(newGraph.edges.flatMap { listOf(it.source, it.target) }.map { it.name to it })
        dominatorTree = DominatorTree.build(
            newGraph,
            IdentityGraphPreprocessor()
        )
        retainedSizes =
            nodes.mapValues { (_, node) ->
                VertexWithType(
                    node.name,
                    dominatorTree.getRetainedSize(node),
                    node.type,
                    node.displayName
                )
            }
    }

    fun writeSizesToConsole() {
        println(json.encodeToString(retainedSizes))
    }

    fun writeSizesJSON(outputFile: File) {
        outputFile.writeText(Json.encodeToString(retainedSizes))
    }

    fun writeSizesJS(outputFile: File) {
        outputFile.writeText(
            """
            | export const kotlinRetainedSize = ${Json.encodeToString(retainedSizes)}
            """.trimMargin().trim()
        )
    }

    val parents = dominatorTree
        .dominators
        .mapKeys { (it, _) -> it.toString() }
        .mapValues { (_, it) -> it.toString() }

    fun writeEdgesToConsole() {
        println(json.encodeToString(parents))
    }

    fun writeEdgesJs(outputFile: File) {
        outputFile.writeText(Json.encodeToString(parents))
    }

    fun writeEdgesJSON(outputFile: File) {
        outputFile.writeText(
            """
            | export const retainedReachibilityInfo = ${Json.encodeToString(parents)}
            """.trimMargin().trim()
        )
    }
}