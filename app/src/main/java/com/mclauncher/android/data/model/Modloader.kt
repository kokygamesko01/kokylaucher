package com.mclauncher.android.data.model

import java.io.File

/**
 * A modloader option (Fabric, Forge, NeoForge, Quilt).
 */
data class Modloader(
    val id: String,              // "fabric", "forge", "neoforge", "quilt"
    val name: String,            // Display name
    val description: String,     // Short description
    val website: String?,        // Official website URL
    val loaderVersions: List<LoaderVersion> = emptyList()
)

/**
 * A specific loader version with Minecraft version compatibility.
 */
data class LoaderVersion(
    val version: String,         // Loader version (e.g. "0.14.21", "47.2.0")
    val minecraftVersion: String,// Compatible Minecraft version
    val stable: Boolean = false,
    val url: String? = null      // Installer/download URL if available
)

/**
 * Persisted user configuration: selected Minecraft version and modloader.
 */
data class SelectedConfig(
    val selectedVersionId: String? = null,
    val selectedVersionType: String? = null,
    val selectedModloader: String? = null,      // "none", "fabric", "forge", "neoforge", "quilt"
    val selectedLoaderVersion: String? = null,
    val selectedLoaderMinecraftVersion: String? = null
)

/**
 * Download progress update.
 */
data class DownloadProgress(
    val url: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val percentage: Int
)

/**
 * Download result.
 */
sealed class DownloadResult {
    data class Success(val file: File) : DownloadResult()
    data class Failure(val exception: Exception) : DownloadResult()
    data class Cancelled(val file: File?) : DownloadResult()
}

/**
 * Result of checking if game files exist for a version/modloader combination.
 */
data class LaunchReadiness(
    val isReady: Boolean,
    val missingComponents: List<String>,
    val javaRuntimePresent: Boolean,
    val lwjglPresent: Boolean,
    val gameFilesPresent: Boolean,
    val assetsPresent: Boolean
)
