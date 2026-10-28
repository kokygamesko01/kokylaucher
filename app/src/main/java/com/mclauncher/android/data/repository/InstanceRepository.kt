package com.mclauncher.android.data.repository

import android.content.Context
import com.mclauncher.android.data.model.Instance
import java.io.File
import java.util.*

/**
 * Repository for managing isolated game instances on the file system.
 * Each instance lives in its own directory under the app's external files dir.
 */
class InstanceRepository(private val context: Context) {

    private val instancesRoot: File

    init {
        // Use external files dir for instance storage (no extra permission needed on modern Android)
        val externalDir = context.getExternalFilesDir(null)
        instancesRoot = if (externalDir != null) {
            File(externalDir, "instances")
        } else {
            File(context.filesDir, "instances")
        }
        if (!instancesRoot.exists()) {
            instancesRoot.mkdirs()
        }
    }

    /**
     * Returns the root directory where all instances are stored.
     */
    fun getInstancesRoot(): File = instancesRoot

    /**
     * Lists all existing instances.
     */
    fun listInstances(): List<Instance> {
        return instancesRoot.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { dir -> loadInstance(dir) }
            .orEmpty()
            .sortedByDescending { it.createdAt }
    }

    /**
     * Loads an [Instance] from its directory by reading instance.json.
     */
    private fun loadInstance(dir: File): Instance? {
        val metaFile = File(dir, "instance.json")
        if (!metaFile.exists()) return null
        try {
            val json = metaFile.readText()
            // Simple manual parse to avoid Gson dependency here
            return parseInstanceJson(json, dir.absolutePath)
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseInstanceJson(json: String, gameDir: String): Instance? {
        // Minimal JSON parsing without Gson for the meta file
        var id = ""
        var name = ""
        var versionId = ""
        var createdAt = 0L
        var modLoader = "none"
        var isFavorite = false

        // Very simple key extraction
        for (line in json.lines()) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("\"id\"") -> id = extractStringValue(trimmed)
                trimmed.startsWith("\"name\"") -> name = extractStringValue(trimmed)
                trimmed.startsWith("\"versionId\"") -> versionId = extractStringValue(trimmed)
                trimmed.startsWith("\"createdAt\"") -> createdAt = extractLongValue(trimmed)
                trimmed.startsWith("\"modLoader\"") -> modLoader = extractStringValue(trimmed)
                trimmed.startsWith("\"isFavorite\"") -> isFavorite = extractBooleanValue(trimmed)
            }
        }

        return if (id.isNotEmpty() && name.isNotEmpty()) {
            Instance(id, name, versionId, createdAt, modLoader, gameDir, isFavorite)
        } else {
            null
        }
    }

    private fun extractStringValue(line: String): String {
        val start = line.indexOf('"', line.indexOf(':') + 1)
        val end = line.indexOf('"', start + 1)
        return if (start != -1 && end != -1) line.substring(start + 1, end) else ""
    }

    private fun extractLongValue(line: String): Long {
        val start = line.indexOf(':') + 1
        val end = line.indexOf(',', start)
        val substr = if (end != -1) line.substring(start, end).trim() else line.substring(start).trim()
        return substr.toLongOrNull() ?: 0L
    }

    private fun extractBooleanValue(line: String): Boolean {
        return line.contains("true")
    }

    /**
     * Creates a new instance with the given name and version.
     */
    fun createInstance(name: String, versionId: String, modLoader: String = "none"): Instance {
        val id = UUID.randomUUID().toString()
        val createdAt = System.currentTimeMillis()
        val instanceDir = File(instancesRoot, id)
        instanceDir.mkdirs()

        // Create standard instance subdirectories
        listOf("mods", "resourcepacks", "saves", "config", "logs", "versions", "libraries", "assets")
            .forEach { File(instanceDir, it).mkdirs() }

        val instance = Instance(id, name, versionId, createdAt, modLoader, instanceDir.absolutePath)
        saveInstance(instance)
        return instance
    }

    /**
     * Saves an instance's metadata to instance.json in its directory.
     */
    fun saveInstance(instance: Instance) {
        val dir = File(instance.gameDir)
        val metaFile = File(dir, "instance.json")
        val json = """{
  "id": "${instance.id}",
  "name": "${instance.name}",
  "versionId": "${instance.versionId}",
  "createdAt": ${instance.createdAt},
  "modLoader": "${instance.modLoader}",
  "isFavorite": ${instance.isFavorite}
}""".trimIndent()
        metaFile.writeText(json)
    }

    /**
     * Deletes an instance and all its files.
     */
    fun deleteInstance(instance: Instance): Boolean {
        val dir = File(instance.gameDir)
        return deleteRecursive(dir)
    }

    private fun deleteRecursive(dir: File): Boolean {
        val files = dir.listFiles() ?: return true
        for (file in files) {
            if (file.isDirectory) {
                deleteRecursive(file)
            } else {
                file.delete()
            }
        }
        return dir.delete()
    }

    /**
     * Gets the instance directory for a given instance ID.
     */
    fun getInstanceDir(instanceId: String): File? {
        return File(instancesRoot, instanceId).takeIf { it.exists() }
    }
}
