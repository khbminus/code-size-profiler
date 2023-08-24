plugins {
    kotlin("jvm") version "1.9.0"
    `java-gradle-plugin`
    `maven-publish`
}

group = "org.jetbrains.kotlin.wasm.sizeprofiler"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation(kotlin("gradle-plugin-api"))
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
            id = "org.jetbrains.kotlin.wasm.sizeprofiler"
            implementationClass = "org.jetbrains.kotlin.wasm.sizeprofiler.gradle.CodeSizeProfilerGradlePlugin"
        }
    }
}