package org.jetbrains.kotlin.wasm.sizeprofiler.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.KotlinJsOptions
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import javax.inject.Inject

abstract class AddCompilerArgsTask @Inject constructor() : DefaultTask() {

    @TaskAction
    private fun addCompilerArgs() {
        project.tasks.withType(Kotlin2JsCompile::class.java)
            .configureEach {
                logger.lifecycle("Injecting profiler compiler arguments into ${it.path}")
                val buildDir = it.destinationDirectory.get().asFile
                val arguments = listOf(
                    "-Xir-dce",
                    "-Xir-dce-dump-reachability-info-to-file=${buildDir.resolve("dce-graph.json")}",
                    "-Xir-dump-declaration-ir-sizes-to-file=${buildDir.resolve("ir-sizes.json")}",
                    "-Xir-dce-print-reachability-info",
                    "-Xwasm-generate-wat",
                    "-Xwasm-debug-info",
                    "-Xwasm-generate-wat-sourcemap",
                    "-Xwasm-dump-dce-declaration-ir-sizes-to-file=${buildDir.resolve("ir-sizes-extended.json")}",
                    "-Xdump-functions-location-in-wat-to-file=${buildDir.resolve("functions-wat.json")}"
                )
                logger.debug(arguments.joinToString(separator = "\n", prefix = "Parameters for ${it.path}:\n"))
                it.kotlinOptions.addCompileFlags(arguments)
            }
    }

    private fun KotlinJsOptions.addCompileFlags(args: List<String>) {
        val currentOptions = freeCompilerArgs.toSet()

        freeCompilerArgs += args.filter { it !in currentOptions }
    }
}