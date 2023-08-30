package org.jetbrains.kotlin.wasm.sizeprofiler.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

abstract class VisualizationDownloaderTask : DefaultTask() {
    private val visualizationLocation = project.buildDir

    @TaskAction
    fun downloadVisualization() {
        if (visualizationLocation.resolve(outputLocation).exists()) {
            logger.info("Found existing visualization. Removing")
            visualizationLocation.resolve(outputLocation).deleteRecursively()
        }
        val visualizationURL = URL(ZIP_FILE_PATH)

        val zipStream = ZipInputStream(visualizationURL.openStream())
        val buffer = ByteArray(BUFFER_SIZE)
        zipStream.use {
            var zipEntry = it.nextEntry
            while (zipEntry != null) {
                println("Uncompressing ${zipEntry.name}")
                val nextFile = createNewFile(zipEntry)
                if (zipEntry.isDirectory) {
                    if (!nextFile.isDirectory && !nextFile.mkdirs()) {
                        error("Failed to create directory $nextFile")
                    }
                } else {
                    nextFile.createParentDirectory()
                    val fos = FileOutputStream(nextFile)
                    var length: Int = zipStream.read(buffer)
                    while (length != -1) {
                        fos.write(buffer, 0, length)
                        length = zipStream.read(buffer)
                    }
                    fos.close()
                }
                zipEntry = it.nextEntry
            }
            it.closeEntry()
        }
    }

    @get:OutputDirectory
    val outputLocation: File
        get() = visualizationLocation.resolve(APP_LOCATION)

    private fun createNewFile(zipEntry: ZipEntry) = File(visualizationLocation, zipEntry.name).also {
        if (ensureZipSlip(it)) {
            error("Can't create files outside root directory. The ZIP archive is corrupted or contains zip Slip vulnerability")
        }
    }

    private fun File.createParentDirectory() {
        if (!parentFile.isDirectory && !parentFile.mkdirs()) {
            error("Failed to create directory $parentFile")
        }
    }

    /**
     * Ensures that the provided file is located within the rootPath directory to prevent Zip Slip vulnerability.
     *
     * @param file The file to check.
     * @return Returns true if the file is located within the rootPath directory, false otherwise.
     */
    private fun ensureZipSlip(file: File): Boolean {
        val rootPath = visualizationLocation.canonicalPath
        val filePath = file.canonicalPath
        return !filePath.startsWith(rootPath)
    }

    companion object {
        private const val COMMIT = "43f084d73b703c526fb6b6d50acf79b58484df7b"
        private const val ZIP_FILE_PATH =
            "https://github.com/khbminus/code-size-profiling-visualization/archive/$COMMIT.zip"
        private const val BUFFER_SIZE = 4096
        const val APP_LOCATION = "code-size-profiling-visualization-$COMMIT"
    }
}