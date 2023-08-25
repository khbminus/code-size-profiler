plugins {
    kotlin("jvm") version "1.9.0"
    `java-gradle-plugin`
    `maven-publish`
}

group = "org.jetbrains.kotlin.wasm.sizeprofiler"
version = "1.9.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    runtimeOnly("org.jetbrains.kotlin.multiplatform:org.jetbrains.kotlin.multiplatform.gradle.plugin:1.9.0")
    api(kotlin("gradle-plugin"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}

gradlePlugin {
    plugins {
        create("wasm-code-size-profiler") {
            id = "org.jetbrains.kotlin.wasm.sizeprofiler.gradle-plugin"
            implementationClass = "org.jetbrains.kotlin.wasm.sizeprofiler.gradle.CodeSizeProfilerGradlePlugin"
            version = project.version.toString()
        }
    }
}