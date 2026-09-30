package com.norabuilder.core

import androidx.documentfile.provider.DocumentFile

class ProjectAnalyzer {
    enum class Status { PASS, WARNING, ERROR }
    data class Check(val name: String, val status: Status, val msg: String)
    data class ValidationResult(val checks: List<Check>, val isValid: Boolean)

    fun analyze(root: DocumentFile): ValidationResult {
        val checks = mutableListOf<Check>()
        fun check(name: String, exists: Boolean, err: String) {
            checks.add(Check(name, if (exists) Status.PASS else Status.ERROR, if (exists) "Found" else err))
        }
        check("Gradle Wrapper", root.findFile("gradlew") != null,
            "Gradle wrapper was not found. Generate or configure a compatible Gradle wrapper.")
        check("settings.gradle",
            root.findFile("settings.gradle") != null || root.findFile("settings.gradle.kts") != null,
            "Missing settings.gradle")
        check("AndroidManifest.xml", findRecursively(root, "AndroidManifest.xml") != null,
            "Manifest not found in app/src/main/")
        check("App Module", root.findFile("app") != null, "App module missing")
        check("Source Directory",
            findRecursively(root, "java") != null || findRecursively(root, "kotlin") != null,
            "No source directory found")
        return ValidationResult(checks, checks.none { it.status == Status.ERROR })
    }

    private fun findRecursively(dir: DocumentFile, name: String, depth: Int = 6): DocumentFile? {
        if (depth < 0) return null
        for (f in dir.listFiles()) {
            if (f.name == name) return f
            if (f.isDirectory) {
                val hit = findRecursively(f, name, depth - 1)
                if (hit != null) return hit
            }
        }
        return null
    }
}
