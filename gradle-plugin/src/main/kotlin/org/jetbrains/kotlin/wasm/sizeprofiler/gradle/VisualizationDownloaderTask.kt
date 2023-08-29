package org.jetbrains.kotlin.wasm.sizeprofiler.gradle

import org.gradle.api.DefaultTask
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
        if (visualizationLocation.resolve(appLocation).exists()) {
            logger.info("Found existing visualization. Removing")
            visualizationLocation.resolve(appLocation).deleteRecursively()
        }
        val visualizationURL = URL(zipFilePath)

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
        private const val zipFilePath =
            "https://github.com/khbminus/code-size-profiling-visualization/archive/refs/heads/master.zip"
        private const val BUFFER_SIZE = 4096
        const val appLocation = "code-size-profiling-visualization-master"
    }
}