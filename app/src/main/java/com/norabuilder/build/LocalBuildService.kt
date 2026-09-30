package com.norabuilder.build

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LocalBuildService {
    sealed class BuildResult {
        data class Success(val apk: File, val durationMs: Long) : BuildResult()
        data class Failure(val error: String, val logs: String = "") : BuildResult()
    }

    fun isJavaAvailable(): Boolean = try {
        ProcessBuilder("java", "-version").redirectErrorStream(true).start().waitFor() == 0
    } catch (e: Exception) {
        false
    }

    suspend fun build(
        projectDir: File,
        variant: String = "assembleDebug",
        onLog: (String) -> Unit
    ): BuildResult = withContext(Dispatchers.IO) {
        val gradlew = File(projectDir, "gradlew")
        if (!gradlew.exists()) {
            return@withContext BuildResult.Failure("Gradle wrapper not found. Local build unavailable.")
        }
        if (!isJavaAvailable()) {
            return@withContext BuildResult.Failure("JDK not available on this device. Use Cloud Build.")
        }
        val start = System.currentTimeMillis()
        try {
            val process = ProcessBuilder("sh", gradlew.absolutePath, variant)
                .directory(projectDir).redirectErrorStream(true).start()
            val logs = StringBuilder()
            process.inputStream.bufferedReader().forEachLine { line ->
                logs.appendLine(line)
                onLog(line)
            }
            val exit = process.waitFor()
            if (exit == 0) {
                val apk = findApk(projectDir)
                    ?: return@withContext BuildResult.Failure(
                        "BUILD SUCCESSFUL but APK not found in app/build/outputs/", logs.toString())
                BuildResult.Success(apk, System.currentTimeMillis() - start)
            } else {
                BuildResult.Failure(parseFailedTask(logs.toString()), logs.toString())
            }
        } catch (e: Exception) {
            BuildResult.Failure(e.message ?: "Unknown error")
        }
    }

    private fun findApk(projectDir: File): File? =
        File(projectDir, "app/build/outputs").walkTopDown().firstOrNull { it.extension == "apk" }

    private fun parseFailedTask(logs: String): String =
        logs.lines().firstOrNull { it.contains("FAILED") } ?: "Build failed (exit code non-zero)"
}
