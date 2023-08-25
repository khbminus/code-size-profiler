package org.jetbrains.kotlin.wasm.sizeprofiler.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class CodeSizeProfilerGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {

    }

    private fun addCompilerArgs(project: Project) {
        project.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        val buildDir = project.buildDir
        val profilerDumps = buildDir.resolve("code-size-profiler-dumps")
        profilerDumps.mkdirs()

        project.tasks.withType(KotlinCompile::class.java)
            .configureEach {
                it.kotlinOptions.freeCompilerArgs += listOf(
                    "-Xir-dce",
                    "-Xir-dce-dump-reachability-info-to-file=${profilerDumps.resolve("dce-graph.json")}",
                    "-Xir-dump-declaration-ir-sizes-to-file=${profilerDumps.resolve("ir-sizes.json")}",
                    "-Xir-dce-print-reachability-info",
                    "-Xwasm-generate-wat",
                    "-Xwasm-debug-info",
                    "-Xwasm-generate-wat-sourcemap",
                    "-Xwasm-dump-dce-declaration-ir-sizes-to-file=${profilerDumps.resolve("ir-sizes-extended.json")}",
                    "-Xdump-functions-location-in-wat-to-file=${profilerDumps.resolve("functions-wat.json")}"
                )
            }
    }
}