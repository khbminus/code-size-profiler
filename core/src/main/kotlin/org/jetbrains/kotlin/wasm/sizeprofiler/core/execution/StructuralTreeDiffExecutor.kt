package org.jetbrains.kotlin.wasm.sizeprofiler.core.execution

import org.jetbrains.kotlin.wasm.sizeprofiler.core.DifferenceTree
import kotlin.system.measureTimeMillis

class StructuralTreeDiffExecutor(treeLeft: DifferenceTree.RetainedTree, treeRight: DifferenceTree.RetainedTree) {
    val differenceTree: DifferenceTree
    init {

        val time = measureTimeMillis {
            differenceTree = DifferenceTree.build(treeLeft, treeRight)
        }
        println("Building compressing tree finished in $time ms")
    }
}