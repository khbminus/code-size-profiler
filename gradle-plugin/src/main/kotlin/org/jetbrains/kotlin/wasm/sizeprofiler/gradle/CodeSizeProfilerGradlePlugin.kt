package org.jetbrains.kotlin.wasm.sizeprofiler.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.configurationcache.extensions.capitalized
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.Executable
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget

abstract class CodeSizeProfilerGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.withId("org.jetbrains.kotlin.multiplatform") {
            val kotlin = project.extensions.findByName("kotlin") as? KotlinMultiplatformExtension
                ?: error("no kotlin extension")
            kotlin.targets.withType(KotlinJsIrTarget::class.java).configureEach {
                it.binaries.withType(Executable::class.java).configureEach skipProduction@{
                    if (it.mode != KotlinJsBinaryMode.DEVELOPMENT) {
                        return@skipProduction
                    }
                    val linkTask = project.tasks.named(it.linkTaskName, KotlinJsIrLink::class.java)
                    val profileTask =
                        project.tasks.register("${it.linkTaskName.capitalized()}-profiled", DefaultTask::class.java) {
                            val buildDir = linkTask.get().destinationDirectory.get().asFile
                            val arguments = listOf(
                                "-Xir-dce",
                                "-Xir-dce-dump-reachability-info-to-file=${buildDir.resolve("dce-graph.json")}",
                                "-Xir-dump-declaration-ir-sizes-to-file=${buildDir.resolve("ir-sizes.json")}",
                                "-Xwasm-generate-wat",
                                "-Xwasm-generate-wat-sourcemap",
                                "-Xwasm-dump-dce-declaration-ir-sizes-to-file=${buildDir.resolve("ir-sizes-extended.json")}",
                                "-Xdump-functions-location-in-wat-to-file=${buildDir.resolve("functions-wat.json")}"
                            )
                            println("Adding arguments to task ${linkTask.name}: ${arguments.joinToString(separator = "\n")}")
                            linkTask.get().kotlinOptions.freeCompilerArgs += arguments
                            it.finalizedBy(linkTask)
                        }

                    val buildDir = project.layout.buildDirectory.get().asFile
                    val downloadVisualization =
                        project.tasks.register("downloadVisualization", VisualizationDownloaderTask::class.java)
                    val processDumpTask = project.tasks.register("processDump", DumpProcessingTask::class.java) {
                        it.inputDir.set(linkTask.get().destinationDirectory.get())
                        it.dependsOn(profileTask)
                        it.dependsOn(linkTask)
                        it.dependsOn(downloadVisualization)
                    }

                    val npmInstallTask = project.tasks.register("visualizationNPMInstall", Exec::class.java) {
                        it.workingDir = buildDir.resolve(VisualizationDownloaderTask.APP_LOCATION)
                        it.commandLine = listOf("npm", "install", "--quiet", "--no-audit", "--no-fund")
                        it.dependsOn(downloadVisualization)
                    }
                    val buildVisualizationTask = project.tasks.register("buildVisualization", Exec::class.java) {
                        it.workingDir = buildDir.resolve(VisualizationDownloaderTask.APP_LOCATION)
                        it.commandLine = listOf("npm", "run", "build")
                        it.dependsOn(npmInstallTask)
                        it.dependsOn(processDumpTask)
                    }
                    project.tasks.register("runVisualization", Exec::class.java) {
                        it.workingDir = buildDir.resolve(VisualizationDownloaderTask.APP_LOCATION)
                        it.commandLine = listOf("npm", "start")
                        it.dependsOn(buildVisualizationTask)
                    }
                }
            }
        }

    }


}