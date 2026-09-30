package com.norabuilder.util

import java.io.File
import java.util.zip.ZipInputStream

object SecurityUtil {
    /** Extracts a zip, rejecting any entry that would escape destDir (Zip Slip). */
    fun safeExtract(zipFile: File, destDir: File) {
        val root = destDir.canonicalPath
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val destFile = File(destDir, entry.name)
                if (!destFile.canonicalPath.startsWith(root + File.separator)) {
                    throw SecurityException("Path traversal detected: ${entry.name}")
                }
                if (entry.isDirectory) {
                    destFile.mkdirs()
                } else {
                    destFile.parentFile?.mkdirs()
                    destFile.outputStream().use { zis.copyTo(it) }
                }
                entry = zis.nextEntry
            }
        }
    }
}
