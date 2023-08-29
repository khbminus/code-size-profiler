package org.jetbrains.kotlin.wasm.sizeprofiler.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

class CodeSizeProfilerGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val downloadVisualization = project.tasks.register("downloadVisualization", VisualizationDownloaderTask::class.java)
        project.tasks.register("processDump", DumpProcessingTask::class.java) {
            val linkTask = project
                .tasks
                .withType(KotlinJsIrLink::class.java)
                .firstOrNull { it.path == ":compileDevelopmentExecutableKotlinWasm" } ?: return@register
            it.inputDir.set(linkTask.destinationDirectory.get())
            it.dependsOn(linkTask)
        }

        val addArgsTask = project.tasks.create("addCompilerArgs", AddCompilerArgsTask::class.java)
        project.tasks.withType(Kotlin2JsCompile::class.java).configureEach {
            it.dependsOn(addArgsTask)
        }
    }


}