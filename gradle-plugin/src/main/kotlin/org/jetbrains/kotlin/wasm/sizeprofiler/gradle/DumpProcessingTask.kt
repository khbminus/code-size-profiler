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
        val fileName = "${project.name}-wasm"
        val inputDirFile = inputDir.get().asFile
        val irSizesFile = inputDirFile.resolve("ir-sizes.json")
        val irSizesExtendedFile = inputDirFile.resolve("ir-sizes-extended.json")
        val dceGraphFile = inputDirFile.resolve("dce-graph.json")
        val sourceMapFile = inputDirFile.resolve("$fileName.map")
        val watSourceMapFile = inputDirFile.resolve("$fileName.wat.map")
        val watFile = inputDirFile.resolve("$fileName.wat")
        val functionLocationsFile = inputDirFile.resolve("functions-wat.json")

        logger.lifecycle("DUMP PROCESSING INPUT DIRECTORY: ${inputDir.get()}")
        logger.lifecycle("IR Sizes: $irSizesFile. Exists? ${irSizesFile.exists()}")
        logger.lifecycle("IR Sizes extended: $irSizesExtendedFile. Exists? ${irSizesExtendedFile.exists()}")
        logger.lifecycle("DCE Graph: $dceGraphFile. Exists? ${dceGraphFile.exists()}")
        logger.lifecycle("sourcemap kt-wasm: $sourceMapFile. Exists? ${sourceMapFile.exists()}")
        logger.lifecycle("sourcemap wat-wasm: $watSourceMapFile. Exists? ${watSourceMapFile.exists()}")
        logger.lifecycle("wat file: $watFile. Exists? ${watFile.exists()}")
        logger.lifecycle("function locations: $functionLocationsFile. Exists? ${functionLocationsFile.exists()}")

        if (!irSizesFile.exists()) {
            error("IR sizes file is missing. Visualization couldn't be made")
        }
        if (!dceGraphFile.exists()) {
            error("DCE graph data is missing. Visualization couldn't be made")
        }
    }
}