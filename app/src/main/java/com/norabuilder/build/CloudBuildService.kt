package com.norabuilder.build

import android.content.SharedPreferences
import com.norabuilder.data.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class CloudBuildService(private val prefs: SharedPreferences) {
    /** Dispatches .github/workflows/build.yml. The token is read from encrypted prefs, never hard-coded. */
    suspend fun triggerBuild(repo: String, branch: String, onStatus: (String) -> Unit) =
        withContext(Dispatchers.IO) {
            val token = prefs.getString(SettingsManager.KEY_GITHUB_TOKEN, null)
                ?: throw IllegalStateException("Cloud authentication not configured. Add a token in Settings.")
            val body = JSONObject().put("ref", branch).toString()
                .toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("https://api.github.com/repos/$repo/actions/workflows/build.yml/dispatches")
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .post(body)
                .build()
            OkHttpClient().newCall(req).execute().use {
                if (!it.isSuccessful) throw IOException("Cloud build failed: HTTP ${it.code}")
            }
            onStatus("Workflow dispatched. Follow it in the repo's Actions tab, then download the artifact.")
        }
}
