package tasks

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import dominator.DominatorTree
import graph.DirectedGraphWithFakeSource
import graph.Edge
import graph.VertexWithType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class Dominators : CliktCommand(help = "Build dominator tree and get retained size") {
    private val json = Json { prettyPrint = true }
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
    private val isParentEdgeFormat by option("--tree", help = "format of edge file").flag(
        "--graph",
        default = false,
        defaultForHelp = "use graph format"
    )
    private val removeUnknown by option(
        "--remove-unknown",
        "-r",
        help = "remove nodes, that are not stated in IR sizes"
    ).flag("--leave-unknown", default = false, defaultForHelp = "leave unknown and add it to result")


    override fun run() {
        val edgeEntries = Json.decodeFromString<List<EdgeEntry>>(graphDataFile.readText())
        val sizes = Json.decodeFromString<Map<String, VertexWithType>>(irSizeFile.readText())
        sizes.forEach { (k, v) -> v.name = k }

        val nodes = sizes.toMutableMap()
        val edges = edgeEntries.filter { it.source != it.target }
            .filter { !removeUnknown || (it.source in nodes && it.target in nodes) }
            .map {
                val source = nodes.getOrPut(it.source) { VertexWithType(it.source, 0, "unknown") }
                val target = nodes.getOrPut(it.target) { VertexWithType(it.target, 0, "unknown") }
                Edge(source, target)
            }
        val dominatorTree = DominatorTree.build(DirectedGraphWithFakeSource(edges))
        val retainedSizes =
            nodes.mapValues { (_, node) -> VertexWithType(node.name, dominatorTree.getRetainedSize(node), node.type) }
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
            val parents = dominatorTree
                .dominators
                .mapKeys { (it, _) -> it.toString() }
                .mapValues { (_, it) -> it.toString() }
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