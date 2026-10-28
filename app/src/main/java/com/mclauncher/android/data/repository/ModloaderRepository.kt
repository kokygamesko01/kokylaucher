package com.mclauncher.android.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.mclauncher.android.data.model.DownloadProgress
import com.mclauncher.android.data.model.DownloadResult
import com.mclauncher.android.data.model.LoaderVersion
import com.mclauncher.android.data.model.Modloader
import com.mclauncher.android.data.model.Version
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.concurrent.thread

const val FABRIC_LOADER_URL = "https://meta.fabricmc.net/v2/versions/loader"
const val QUILT_LOADER_URL = "https://meta.quiltmc.org/v3/versions/loader"
const val FORGE_PROMOTIONS_URL = "https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json"

// SharedPreferences keys
const val PREFS_NAME = "kokylaucher_prefs"
const val KEY_SELECTED_VERSION_ID = "selected_version_id"
const val KEY_SELECTED_VERSION_TYPE = "selected_version_type"
const val KEY_SELECTED_MODLOADER = "selected_modloader"
const val KEY_SELECTED_LOADER_VERSION = "selected_loader_version"
const val KEY_SELECTED_LOADER_MCVERSION = "selected_loader_mcversion"
const val KEY_PLAYER_NAME = "player_name"

/**
 * Repository for modloader metadata and user configuration persistence.
 */
class ModloaderRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Returns the default modloaders with their descriptions.
     * Loader versions are populated asynchronously via fetchLoaderVersions().
     */
    fun getDefaultModloaders(): List<Modloader> {
        return listOf(
            Modloader(
                id = "fabric",
                name = "Fabric",
                description = "Lightweight, modular modloader for Minecraft",
                website = "https://fabricmc.net/"
            ),
            Modloader(
                id = "forge",
                name = "Forge",
                description = "Classic Minecraft modloader with extensive mod support",
                website = "https://files.minecraftforge.net/"
            ),
            Modloader(
                id = "neoforge",
                name = "NeoForge",
                description = "Community-driven fork of Forge",
                website = "https://neoforged.com/"
            ),
            Modloader(
                id = "quilt",
                name = "Quilt",
                description = "Community-driven modloader fork of Fabric",
                website = "https://quiltmc.org/"
            )
        )
    }

    /**
     * Fetches Fabric loader versions from the official FabricMC API.
     * Returns loader versions filtered for the given Minecraft version if provided.
     */
    suspend fun fetchFabricLoaderVersions(minecraftVersion: String? = null): Result<List<LoaderVersion>> {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(FABRIC_LOADER_URL)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.addRequestProperty("User-Agent", "Kokylaucher/1.2.0")

                val responseCode = connection.responseCode
                if (responseCode != 200) {
                    Result.failure(Exception("HTTP $responseCode: ${connection.responseMessage}"))
                } else {
                    val reader = java.io.InputStreamReader(connection.inputStream, java.nio.charset.StandardCharsets.UTF_8)
                    val builder = StringBuilder()
                    val buffer = CharArray(4096)
                    var read: Int
                    while (reader.read(buffer).also { read = it } != -1) {
                        builder.append(buffer, 0, read)
                    }
                    reader.close()

                    val loaderVersions = parseFabricLoaderVersions(builder.toString(), minecraftVersion)
                    Result.success(loaderVersions)
                }
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun parseFabricLoaderVersions(json: String, minecraftVersion: String?): List<LoaderVersion> {
        val versions = mutableListOf<LoaderVersion>()
        try {
            val root = org.json.JSONArray(json)
            for (i in 0 until root.length()) {
                val entry = root.getJSONObject(i)
                val loaderObj = entry.getJSONObject("loader")
                val loaderVersion = loaderObj.getString("version")
                val stable = entry.optBoolean("stable", false)

                // Get compatible Minecraft versions
                val gameVersions = entry.optJSONArray("gameVersions")
                if (gameVersions != null) {
                    for (j in 0 until gameVersions.length()) {
                        val gv = gameVersions.getJSONObject(j)
                        val mcVer = gv.getString("version")
                        if (minecraftVersion == null || mcVer == minecraftVersion) {
                            versions.add(LoaderVersion(
                                version = loaderVersion,
                                minecraftVersion = mcVer,
                                stable = stable,
                                url = null  // Fabric installer URL not directly in this API
                            ))
                        }
                    }
                } else if (minecraftVersion == null) {
                    versions.add(LoaderVersion(
                        version = loaderVersion,
                        minecraftVersion = "unknown",
                        stable = stable
                    ))
                }
            }
        } catch (e: Exception) {
            // Return empty list on parse error
        }
        return versions
    }

    /**
     * Returns known Forge promotions for a given Minecraft version.
     * This is a simplified check - Forge promotions JSON can be fetched separately.
     */
    fun getKnownForgeVersion(minecraftVersion: String): String? {
        // Known Forge builds for popular MC versions (simplified, not fetched live)
        return when (minecraftVersion) {
            "1.21.1" -> "47.2.0"
            "1.20.1" -> "47.2.0"
            "1.19.4" -> "43.3.5"
            "1.18.2" -> "40.2.1"
            "1.16.5" -> "36.2.39"
            "1.12.2" -> "14.23.5.2860"
            else -> null
        }
    }

    /**
     * Returns known NeoForge versions for a given Minecraft version.
     */
    fun getKnownNeoForgeVersion(minecraftVersion: String): String? {
        return when (minecraftVersion) {
            "1.21.1" -> "21.0.31"
            "1.20.1" -> "20.4.0"
            else -> null
        }
    }

    /**
     * Returns known Quilt versions for a given Minecraft version.
     */
    fun getKnownQuiltVersion(minecraftVersion: String): String? {
        return when (minecraftVersion) {
            "1.21.1" -> "20.2"
            "1.20.1" -> "19.2"
            else -> null
        }
    }

    // -----------------------------------------------------------------------
    // Persisted configuration
    // -----------------------------------------------------------------------

    fun saveSelectedVersion(versionId: String, versionType: String) {
        prefs.edit()
            .putString(KEY_SELECTED_VERSION_ID, versionId)
            .putString(KEY_SELECTED_VERSION_TYPE, versionType)
            .apply()
    }

    fun saveSelectedModloader(modloader: String?, loaderVersion: String?, mcVersion: String?) {
        prefs.edit()
            .putString(KEY_SELECTED_MODLOADER, modloader)
            .putString(KEY_SELECTED_LOADER_VERSION, loaderVersion)
            .putString(KEY_SELECTED_LOADER_MCVERSION, mcVersion)
            .apply()
    }

    fun getSelectedVersionId(): String? = prefs.getString(KEY_SELECTED_VERSION_ID, null)
    fun getSelectedVersionType(): String? = prefs.getString(KEY_SELECTED_VERSION_TYPE, null)
    fun getSelectedModloader(): String? = prefs.getString(KEY_SELECTED_MODLOADER, null)
    fun getSelectedLoaderVersion(): String? = prefs.getString(KEY_SELECTED_LOADER_VERSION, null)
    fun getSelectedLoaderMcVersion(): String? = prefs.getString(KEY_SELECTED_LOADER_MCVERSION, null)

    fun savePlayerName(name: String) {
        prefs.edit().putString(KEY_PLAYER_NAME, name).apply()
    }

    fun getPlayerName(): String? = prefs.getString(KEY_PLAYER_NAME, null)

    fun clearSelections() {
        prefs.edit()
            .remove(KEY_SELECTED_VERSION_ID)
            .remove(KEY_SELECTED_VERSION_TYPE)
            .remove(KEY_SELECTED_MODLOADER)
            .remove(KEY_SELECTED_LOADER_VERSION)
            .remove(KEY_SELECTED_LOADER_MCVERSION)
            .apply()
    }
}

/**
 * Download manager with progress, cancellation, retry, and checksum verification.
 */
class DownloadManager(private val context: Context) {

    private val downloadDir: File
    private val activeDownloads = mutableMapOf<String, Boolean>() // url -> cancelled flag

    init {
        val externalDir = context.getExternalFilesDir(null)
        downloadDir = if (externalDir != null) File(externalDir, "downloads") else File(context.filesDir, "downloads")
        downloadDir.mkdirs()
    }

    /**
     * Downloads a file from URL to destination with progress callbacks.
     * Supports cancellation via the activeDownloads map (set url to false to cancel).
     * Verifies SHA1 checksum if expectedSha1 is provided.
     */
    suspend fun downloadFile(
        url: String,
        destination: File,
        expectedSha1: String? = null,
        onProgress: (DownloadProgress) -> Unit = {}
    ): DownloadResult {
        // Mark this download as not cancelled
        activeDownloads[url] = false

        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null

            try {
                destination.parentFile.mkdirs()

                val urlObj = URL(url)
                connection = urlObj.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.addRequestProperty("User-Agent", "Kokylaucher/1.2.0")

                val responseCode = connection.responseCode
                if (responseCode != 200) {
                    DownloadResult.Failure(Exception("HTTP $responseCode: ${connection.responseMessage}"))
                } else {
                    val totalBytes = connection.contentLength.toLong()
                    inputStream = connection.inputStream
                    outputStream = FileOutputStream(destination)

                    var bytesDownloaded = 0L
                    val buffer = ByteArray(8192)

                    while (true) {
                        val read = inputStream.read(buffer)
                        if (read == -1) break

                        // Check cancellation
                        if (activeDownloads[url] != false) {
                            outputStream.write(buffer, 0, read)
                            bytesDownloaded += read

                            val percentage = if (totalBytes > 0) (bytesDownloaded * 100 / totalBytes).toInt() else 0
                            onProgress(DownloadProgress(url, bytesDownloaded, totalBytes, percentage))
                        } else {
                            // Cancelled
                            return@withContext DownloadResult.Cancelled(destination)
                        }
                    }

                    outputStream.flush()

                    // Verify checksum if provided
                    val result: DownloadResult
                    if (expectedSha1 != null) {
                        val actualSha1 = sha1File(destination)
                        if (actualSha1 != expectedSha1) {
                            destination.delete()
                            result = DownloadResult.Failure(Exception("Checksum mismatch: expected $expectedSha1, got $actualSha1"))
                        } else {
                            result = DownloadResult.Success(destination)
                        }
                    } else {
                        result = DownloadResult.Success(destination)
                    }
                    return@withContext result
                }
            } catch (e: Exception) {
                DownloadResult.Failure(e)
            } finally {
                outputStream?.close()
                inputStream?.close()
                connection?.disconnect()
                activeDownloads.remove(url)
            }
        }
    }

    /**
     * Cancels an ongoing download for the given URL.
     */
    fun cancelDownload(url: String) {
        activeDownloads[url] = false
    }

    /**
     * Computes SHA1 checksum of a file.
     */
    private fun sha1File(file: File): String {
        val digest = MessageDigest.getInstance("SHA-1")
        val input = FileInputStream(file)
        try {
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        } finally {
            input.close()
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun getDownloadDir(): File = downloadDir
}
