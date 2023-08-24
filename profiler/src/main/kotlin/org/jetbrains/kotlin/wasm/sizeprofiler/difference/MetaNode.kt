package org.jetbrains.kotlin.wasm.sizeprofiler.difference

class MetaNode(status: DifferenceStatus, val children: List<DifferenceVertex>) : DifferenceVertex(status)