package org.jetbrains.kotlin.wasm.sizeprofiler.tasks

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.wasm.sizeprofiler.core.DifferenceTree
import org.jetbrains.kotlin.wasm.sizeprofiler.core.execution.StructuralGraphDiffExecutor
import org.jetbrains.kotlin.wasm.sizeprofiler.core.execution.StructuralTreeDiffExecutor
import org.jetbrains.kotlin.wasm.sizeprofiler.core.graph.VertexWithType
import kotlin.io.path.readText
import kotlin.io.path.writeText

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

    private val metaNodeBuild by option("--metanode", help = "Build metanodes information and build compression graph")
        .flag(
            "--no-metanode",
            default = false,
            defaultForHelp = "Disabled due to bad performance"
        )

    override fun run() {
        if (isTree) {
            compareTree()
        } else {
            compareGraph()
        }
    }

    private fun compareTree() {
        val fakeSource = mapOf("Fake source" to VertexWithType("Fake source", 0, "fake source"))
        val treeLeft = DifferenceTree.RetainedTree(
            Json.decodeFromString<Map<String, VertexWithType>>(sizeFileLeft.readText()) + fakeSource,
            Json.decodeFromString<Map<String, String>>(graphDataLeft.readText())
        )
        val treeRight = DifferenceTree.RetainedTree(
            Json.decodeFromString<Map<String, VertexWithType>>(sizeFileRight.readText()) + fakeSource,
            Json.decodeFromString<Map<String, String>>(graphDataRight.readText())
        )
        val executor = StructuralTreeDiffExecutor(treeLeft, treeRight)

        val (extension, parentsPrefix, sizesPrefix) = if (jsOutput) listOf(
            "js",
            "export const diffTreeParents = ",
            "export const diffDeclarationsSizes = "
        ) else listOf("json", "", "")
        outputDirectory?.let {
            val parentsFile = it.resolve("parents.$extension")
            parentsFile.writeText("$parentsPrefix${Json.encodeToString(executor.differenceTree.parents)}")
            val nodesFile = it.resolve("ir-sizes.$extension")
            nodesFile.writeText("$sizesPrefix${Json.encodeToString(executor.differenceTree.nodes)}")
        }
    }

    private fun compareGraph() {
        val graphLeft = StructuralGraphDiffExecutor.GraphData(sizeFileLeft, graphDataLeft)
        val graphRight = StructuralGraphDiffExecutor.GraphData(sizeFileRight, graphDataRight)
        val executor = StructuralGraphDiffExecutor(graphLeft, graphRight)
        if (metaNodeBuild) {
            executor.buildMetaNodes()
        }
        outputDirectory?.toFile()?.let {
            executor.outputGraph(it, jsOutput)
        }
    }
}