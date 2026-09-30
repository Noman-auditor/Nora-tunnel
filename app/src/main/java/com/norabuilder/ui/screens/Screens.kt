package com.norabuilder.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.norabuilder.apk.InstallService
import com.norabuilder.build.CloudBuildService
import com.norabuilder.build.LocalBuildService
import com.norabuilder.core.ProjectAnalyzer
import com.norabuilder.data.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
private fun StatusRow(label: String, value: String, ok: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value, color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
    }
}

@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    var javaOk by remember { mutableStateOf<Boolean?>(null) }
    var tokenSet by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            javaOk = LocalBuildService().isJavaAvailable()
            tokenSet = runCatching {
                SettingsManager.securePrefs(context).contains(SettingsManager.KEY_GITHUB_TOKEN)
            }.getOrDefault(false)
        }
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("NORA BUILDER", style = MaterialTheme.typography.headlineMedium)
        Text("Environment check", style = MaterialTheme.typography.titleMedium)
        StatusRow("Android", "API ${Build.VERSION.SDK_INT}", true)
        StatusRow(
            "JDK (local build)",
            when (javaOk) { null -> "Checking..."; true -> "Ready"; false -> "Unavailable - use Cloud Build" },
            javaOk == true
        )
        StatusRow("Cloud token", if (tokenSet) "Configured" else "Needs setup", tokenSet)
    }
}

@Composable
fun ProjectImportScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf<ProjectAnalyzer.ValidationResult?>(null) }
    var busy by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val root = DocumentFile.fromTreeUri(context, uri)
            if (root != null) {
                busy = true
                scope.launch {
                    result = withContext(Dispatchers.IO) { ProjectAnalyzer().analyze(root) }
                    busy = false
                }
            }
        }
    }
    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Import project", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = { picker.launch(null) }) { Text("Select project folder") }
        if (busy) CircularProgressIndicator()
        result?.let { r ->
            Text(if (r.isValid) "Project looks valid" else "Problems found", style = MaterialTheme.typography.titleMedium)
            r.checks.forEach { c -> Text("${c.status}: ${c.name} - ${c.msg}") }
        }
    }
}

@Composable
fun BuildConsoleScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var projectPath by remember { mutableStateOf("") }
    var repo by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("main") }
    var trusted by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val logs = remember { mutableStateListOf<String>() }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Build console", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(projectPath, { projectPath = it }, label = { Text("Local project path") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(repo, { repo = it }, label = { Text("GitHub repo (owner/name)") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(branch, { branch = it }, label = { Text("Branch") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(trusted, { trusted = it })
            Text("I trust this project's code", fontSize = 13.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = trusted && !busy && projectPath.isNotBlank(), onClick = {
                busy = true
                logs.clear()
                scope.launch {
                    when (val r = LocalBuildService().build(File(projectPath)) { logs.add(it) }) {
                        is LocalBuildService.BuildResult.Success -> {
                            withContext(Dispatchers.IO) {
                                val dir = File(context.getExternalFilesDir(null), "apks").apply { mkdirs() }
                                r.apk.copyTo(File(dir, r.apk.name), overwrite = true)
                            }
                            logs.add("BUILD SUCCESSFUL: ${r.apk.path} (${r.durationMs / 1000}s). See Files tab.")
                        }
                        is LocalBuildService.BuildResult.Failure -> logs.add("FAILED: ${r.error}")
                    }
                    busy = false
                }
            }) { Text("Local build") }
            OutlinedButton(enabled = trusted && !busy && repo.isNotBlank(), onClick = {
                busy = true
                scope.launch {
                    try {
                        val prefs = withContext(Dispatchers.IO) { SettingsManager.securePrefs(context) }
                        CloudBuildService(prefs).triggerBuild(repo.trim(), branch.trim()) { logs.add(it) }
                    } catch (e: Exception) {
                        logs.add("Cloud build error: ${e.message}")
                    }
                    busy = false
                }
            }) { Text("Cloud build") }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            items(logs) { line -> Text(line, fontFamily = FontFamily.Monospace, fontSize = 11.sp) }
        }
    }
}

@Composable
fun FileExplorerScreen() {
    val context = LocalContext.current
    val dir = remember { File(context.getExternalFilesDir(null), "apks").apply { mkdirs() } }
    var files by remember { mutableStateOf(listOf<File>()) }
    fun refresh() { files = dir.listFiles { f -> f.extension == "apk" }?.toList() ?: emptyList() }
    LaunchedEffect(Unit) { refresh() }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("APK manager", style = MaterialTheme.typography.headlineSmall)
        OutlinedButton(onClick = { refresh() }) { Text("Refresh") }
        if (files.isEmpty()) Text("No APKs yet. Local builds are copied here.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(files) { f ->
                Column {
                    Text(f.name)
                    Text("${f.length() / 1024} KB", fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { InstallService(context).install(f) }) { Text("Install") }
                        OutlinedButton(onClick = { f.delete(); refresh() }) { Text("Delete") }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var token by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Text("GitHub token (stored encrypted on this device)")
        OutlinedTextField(token, { token = it }, label = { Text("Token") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = token.isNotBlank(), onClick = {
                scope.launch {
                    status = try {
                        withContext(Dispatchers.IO) {
                            SettingsManager.securePrefs(context).edit()
                                .putString(SettingsManager.KEY_GITHUB_TOKEN, token.trim()).apply()
                        }
                        token = ""
                        "Token saved"
                    } catch (e: Exception) { "Could not save: ${e.message}" }
                }
            }) { Text("Save") }
            OutlinedButton(onClick = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        SettingsManager.securePrefs(context).edit().remove(SettingsManager.KEY_GITHUB_TOKEN).apply()
                    }
                    status = "Token removed"
                }
            }) { Text("Remove") }
        }
        if (status.isNotEmpty()) Text(status)
    }
}
