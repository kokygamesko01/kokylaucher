package com.mclauncher.android.data.model

/**
 * A single Minecraft version entry from the version manifest.
 */
data class Version(
    val id: String,
    val type: String,           // "release", "snapshot", "old_beta", "old_alpha"
    val url: String,            // URL to the version JSON
    val time: String,           // Release time
    val releaseTime: String,
    val isCustom: Boolean = false
)

/**
 * The version manifest response from Mojang's servers.
 */
data class VersionManifest(
    val latest: Latest,
    val versions: List<Version>
)

data class Latest(
    val release: String,
    val snapshot: String
)

/**
 * A Minecraft version's detailed JSON, parsed from the per-version URL.
 */
data class VersionDetail(
    val id: String,
    val type: String,
    val mainClass: String,
    val minimumLauncherVersion: Int,
    val assets: String,
    val assetIndex: AssetIndex,
    val downloads: Downloads,
    val libraries: List<Library>,
    val arguments: Arguments?,
    val javaVersion: JavaVersion?
)

data class AssetIndex(
    val id: String,
    val sha1: String,
    val size: Int,
    val totalSize: Int,
    val url: String
)

data class Downloads(
    val client: DownloadInfo?,
    val server: DownloadInfo?,
    val clientMappings: DownloadInfo?,
    val serverMappings: DownloadInfo?
)

data class DownloadInfo(
    val sha1: String,
    val size: Int,
    val url: String
)

data class Library(
    val name: String,
    val url: String?,
    val downloads: LibraryDownloads?,
    val rules: List<LibraryRule>?,
    val natives: LibraryNatives?
)

data class LibraryDownloads(
    val artifact: DownloadInfo?,
    val classifiers: Map<String, DownloadInfo>?
)

data class LibraryRule(
    val action: String,  // "allow" or "deny"
    val os: LibraryOs?
)

data class LibraryOs(
    val name: String,    // "windows", "osx", "linux"
    val arch: String?
)

data class LibraryNatives(
    val windows: String?,
    val osx: String?,
    val linux: String?
)

data class Arguments(
    val game: List<Any>,
    val jvm: List<Any>
)

data class JavaVersion(
    val component: String,
    val majorVersion: Int
)

/**
 * A game instance — an isolated installation with its own version, mods, etc.
 */
data class Instance(
    val id: String,
    val name: String,
    val versionId: String,
    val createdAt: Long,
    val modLoader: String = "none",  // "none", "fabric", "forge", "quilt"
    val gameDir: String,              // absolute path to instance directory
    val isFavorite: Boolean = false
)

/**
 * A mod file entry.
 */
data class Mod(
    val id: String,
    val name: String,
    val version: String,
    val fileName: String,
    val size: Long,
    val installed: Boolean = false
)

/**
 * A resource pack entry.
 */
data class ResourcePack(
    val id: String,
    val name: String,
    val fileName: String,
    val size: Long,
    val installed: Boolean = false
)
