package org.jetbrains.kotlin.wasm.sizeprofiler.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

class CodeSizeProfilerGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val buildDir = project.layout.buildDirectory.get().asFile
        val downloadVisualization = project.tasks.register("downloadVisualization", VisualizationDownloaderTask::class.java)
        val processDumpTask = project.tasks.register("processDump", DumpProcessingTask::class.java) {
            val linkTask = project
                .tasks
                .withType(KotlinJsIrLink::class.java)
                .firstOrNull { it.path == ":compileDevelopmentExecutableKotlinWasm" } ?: return@register
            it.inputDir.set(linkTask.destinationDirectory.get())
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

        val addArgsTask = project.tasks.create("addCompilerArgs", AddCompilerArgsTask::class.java)
        project.tasks.withType(Kotlin2JsCompile::class.java).configureEach {
            it.dependsOn(addArgsTask)
        }


    }


}