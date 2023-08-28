package org.jetbrains.kotlin.wasm.sizeprofiler.core

class MetaNode(status: DifferenceStatus, val children: List<DifferenceVertex>) : DifferenceVertex(status)