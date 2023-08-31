plugins {
    kotlin("jvm") version "1.9.0"
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.1.0"
    kotlin("plugin.serialization") version "1.9.0"
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.jetbrains.kotlin.wasm.sizeprofiler"
version = "1.0"

repositories {
    mavenCentral()
}

tasks.shadowJar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveClassifier = null
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    shadow(kotlin("gradle-plugin"))
    shadow(gradleApi())

    implementation(project(":core"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    repositories {
        maven {
            name = "localPluginRepository"
            url = mavenLocal().url
        }
    }
}


kotlin {
    jvmToolchain(11)
}

gradlePlugin {
    plugins {
        create("wasm-code-size-profiler") {
            id = "org.jetbrains.kotlin.wasm.sizeprofiler"
            implementationClass = "org.jetbrains.kotlin.wasm.sizeprofiler.gradle.CodeSizeProfilerGradlePlugin"
            version = project.version.toString()
        }
    }
}