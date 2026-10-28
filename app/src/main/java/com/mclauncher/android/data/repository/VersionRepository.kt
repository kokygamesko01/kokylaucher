package com.mclauncher.android.data.repository

import android.content.Context
import com.mclauncher.android.data.model.Latest
import com.mclauncher.android.data.model.Version
import com.mclauncher.android.data.model.VersionManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

const val VERSION_MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json"

/**
 * Repository for fetching Minecraft version data from Mojang's servers.
 * Uses Android's built-in HttpURLConnection and org.json to avoid external dependencies.
 */
class VersionRepository(private val context: Context) {

    /**
     * Fetches the version manifest from Mojang and returns the list of available versions.
     */
    suspend fun fetchVersions(): Result<List<Version>> {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(VERSION_MANIFEST_URL)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.addRequestProperty("User-Agent", "MCLauncher/1.0")

                val responseCode = connection.responseCode
                if (responseCode != 200) {
                    Result.failure(Exception("HTTP $responseCode: ${connection.responseMessage}"))
                } else {
                    val reader = InputStreamReader(connection.inputStream, java.nio.charset.StandardCharsets.UTF_8)
                    val builder = StringBuilder()
                    val buffer = CharArray(4096)
                    var read: Int
                    while (reader.read(buffer).also { read = it } != -1) {
                        builder.append(buffer, 0, read)
                    }
                    reader.close()

                    val manifest = parseManifest(builder.toString())
                    Result.success(manifest.versions)
                }
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun parseManifest(json: String): VersionManifest {
        val root = JSONObject(json)
        val latestObj = root.getJSONObject("latest")
        val latest = Latest(
            release = latestObj.getString("release"),
            snapshot = latestObj.getString("snapshot")
        )

        val versionsArray = root.getJSONArray("versions")
        val versions = mutableListOf<Version>()
        for (i in 0 until versionsArray.length()) {
            val v = versionsArray.getJSONObject(i)
            versions.add(Version(
                id = v.getString("id"),
                type = v.getString("type"),
                url = v.getString("url"),
                time = v.optString("time", ""),
                releaseTime = v.optString("releaseTime", "")
            ))
        }

        return VersionManifest(latest = latest, versions = versions)
    }
}
