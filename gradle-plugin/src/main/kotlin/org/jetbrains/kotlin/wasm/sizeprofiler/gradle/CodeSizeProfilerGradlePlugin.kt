package org.jetbrains.kotlin.wasm.sizeprofiler.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.*

class CodeSizeProfilerGradlePlugin : KotlinCompilerPluginSupportPlugin {
    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        TODO("Not yet implemented")
    }

    override fun getCompilerPluginId(): String = PLUGIN_ID

    override fun getPluginArtifact() = SubpluginArtifact(
        groupId = PLUGIN_ID,
        artifactId = GRADLE_ARTIFACT_ID,
        version = null
    )

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>) = kotlinCompilation.platformType == KotlinPlatformType.wasm
}