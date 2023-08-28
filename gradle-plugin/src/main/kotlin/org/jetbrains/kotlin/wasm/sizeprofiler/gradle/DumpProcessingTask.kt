package org.jetbrains.kotlin.wasm.sizeprofiler.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction


abstract class DumpProcessingTask : DefaultTask() {
    @get:InputDirectory
    abstract val inputDir: DirectoryProperty

    @TaskAction
    fun processDumps() {
        println("DUMP PROCESSING INPUT DIRECTORY: ${inputDir.get()}")
    }
}