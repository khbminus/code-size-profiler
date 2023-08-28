package org.jetbrains.kotlin.wasm.sizeprofiler.core.sourcemaps

import kotlinx.serialization.Serializable

@Serializable
data class FileCursor(val line: Int, val column: Int) : Comparable<FileCursor> {
    override fun compareTo(other: FileCursor): Int {
        val result = line.compareTo(other.line)
        return if (result == 0) column.compareTo(other.column) else result
    }
}