package com.mclauncher.android.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.mclauncher.android.data.model.Instance
import com.mclauncher.android.data.model.LaunchReadiness
import com.mclauncher.android.data.model.Version
import com.mclauncher.android.data.repository.InstanceRepository
import com.mclauncher.android.data.repository.VersionRepository
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Result of a launch attempt.
 */
sealed class LaunchResult {
    data class Success(val processId: Int) : LaunchResult()
    data class Failure(val exception: Throwable, val log: String) : LaunchResult()
    data class MissingRuntime(val message: String) : LaunchResult()
    data class MissingNatives(val message: String) : LaunchResult()
}

/**
 * LaunchManager handles the genuine Minecraft Java Edition launch pipeline:
 * 1. Checks for Android-compatible Java runtime
 * 2. Checks for Android-native LWJGL libraries
 * 3. Assembles the launch command from Minecraft version JSON
 * 4. Executes the Java process and captures logs
 */
class LaunchManager(private val context: Context) {

    private val instanceRepository = InstanceRepository(context)
    private val versionRepository = VersionRepository(context)

    private val launchLog = ConcurrentLinkedQueue<String>()

    private val javaHome: File
    private val lwjglDir: File

    init {
        val filesDir = context.filesDir
        javaHome = File(filesDir, "java-runtime")
        lwjglDir = File(filesDir, "lwjgl-android")
    }

    /**
     * Logs a message to the internal launch log.
     */
    fun log(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        launchLog.add("[$timestamp] $message")
    }

    /**
     * Returns the full launch log as a single string.
     */
    fun getLaunchLog(): String {
        return launchLog.joinToString("\n")
    }

    /**
     * Clears the launch log.
     */
    fun clearLog() {
        launchLog.clear()
    }

    /**
     * Checks whether an Android-compatible Java runtime is available.
     */
    fun isJavaRuntimeAvailable(): Boolean {
        val javaBin = File(javaHome, "bin/java")
        return javaBin.exists() && javaBin.canExecute()
    }

    /**
     * Checks whether Android-native LWJGL libraries are available.
     */
    fun isLwjglAvailable(): Boolean {
        return lwjglDir.exists() && lwjglDir.listFiles()?.any { it.extension == "so" } == true
    }

    /**
     * Checks launch readiness for a given version/modloader combination.
     * Returns which components are missing.
     */
    fun checkLaunchReadiness(
        versionId: String,
        modloader: String?,
        loaderVersion: String?
    ): LaunchReadiness {
        val missing = mutableListOf<String>()

        val javaPresent = isJavaRuntimeAvailable()
        if (!javaPresent) {
            missing.add("Java runtime (JVM pro Android) - $javaHome")
        }

        val lwjglPresent = isLwjglAvailable()
        if (!lwjglPresent) {
            missing.add("Android-native LWJGL knihovny - $lwjglDir")
        }

        // Check game files
        val instancesRoot = instanceRepository.getInstancesRoot()
        val gameJar = File(instancesRoot, "versions/$versionId.jar")
        val gameFilesPresent = gameJar.exists()
        if (!gameFilesPresent) {
            missing.add("Minecraft herní soubory (verze $versionId) - $gameJar")
        }

        // Check assets
        val assetsDir = File(instancesRoot, "assets")
        val assetsPresent = assetsDir.exists() && assetsDir.listFiles()?.isNotEmpty() == true
        if (!assetsPresent) {
            missing.add("Herní assety - $assetsDir")
        }

        // Check modloader if modded
        if (modloader != null && modloader != "none") {
            val modloaderDir = File(instancesRoot, "modloaders/$modloader")
            val modloaderPresent = modloaderDir.exists() && modloaderDir.listFiles()?.isNotEmpty() == true
            if (!modloaderPresent) {
                missing.add("Modloader $modloader (verze $loaderVersion) - $modloaderDir")
            }
        }

        return LaunchReadiness(
            isReady = missing.isEmpty(),
            missingComponents = missing,
            javaRuntimePresent = javaPresent,
            lwjglPresent = lwjglPresent,
            gameFilesPresent = gameFilesPresent,
            assetsPresent = assetsPresent
        )
    }

    /**
     * Attempts to launch Minecraft for the given instance.
     * Returns a [LaunchResult] indicating success or failure.
     */
    fun launch(instance: Instance, selectedVersion: Version?): LaunchResult {
        log("=== Kokylaucher Minecraft Launch ===")
        log("Instance: ${instance.name} (${instance.id})")
        log("Version: ${instance.versionId}")
        log("Launcher type: ${instance.modLoader}")
        log("Java home: ${javaHome.absolutePath}")
        log("LWJGL dir: ${lwjglDir.absolutePath}")

        // 1. Check Java runtime
        if (!isJavaRuntimeAvailable()) {
            log("ERROR: Android-compatible Java runtime not found at $javaHome")
            return LaunchResult.MissingRuntime(
                "Android-compatible Java runtime not found.\n\n" +
                "Kokylaucher needs a JVM port for Android (e.g. Boardwalk/OpenJDK aarch64) " +
                "to run Minecraft: Java Edition. The runtime must be downloaded separately.\n\n" +
                "See: https://github.com/kokygamesko01/kokylaucher#runtime-requirements"
            )
        }
        log("OK: Java runtime found")

        // 2. Check LWJGL natives
        if (!isLwjglAvailable()) {
            log("ERROR: Android-native LWJGL not found at $lwjglDir")
            return LaunchResult.MissingNatives(
                "Android-native LWJGL libraries not found.\n\n" +
                "Minecraft's LWJGL natives are built for desktop (x86/x64) and cannot run on Android ARM64.\n" +
                "Android-native LWJGL must be downloaded separately.\n\n" +
                "See: https://github.com/kokygamesko01/kokylaucher#runtime-requirements"
            )
        }
        log("OK: LWJGL natives found")

        // 3. Build launch command
        val command = buildLaunchCommand(instance, selectedVersion)
        if (command.isEmpty()) {
            val ex = IllegalStateException("Failed to build launch command")
            log("ERROR: ${ex.message}")
            return LaunchResult.Failure(ex, getLaunchLog())
        }
        log("Launch command: ${command.joinToString(" ")}")

        // 4. Execute
        return executeLaunch(command)
    }

    /**
     * Builds the Java launch command for Minecraft.
     */
    private fun buildLaunchCommand(instance: Instance, version: Version?): List<String> {
        val command = mutableListOf<String>()

        // Java executable
        command.add(File(javaHome, "bin/java").absolutePath)

        // JVM arguments
        command.add("-Xmx1G")
        command.add("-Xms512M")
        command.add("-Djava.library.path=${lwjglDir.absolutePath}")
        command.add("-Dminecraft.launcher.brand=kokylaucher")
        command.add("-Dminecraft.launcher.version=1.1.0")

        // Classpath: LWJGL + game jar + libraries
        val classpath = mutableListOf<String>()
        classpath.add(lwjglDir.absolutePath)

        // Add instance libraries
        val librariesDir = File(instance.gameDir, "libraries")
        if (librariesDir.exists()) {
            librariesDir.listFiles()?.filter { it.extension == "jar" }?.forEach {
                classpath.add(it.absolutePath)
            }
        }

        // Add game jar
        val gameJar = File(instance.gameDir, "versions/${instance.versionId}.jar")
        if (gameJar.exists()) {
            classpath.add(gameJar.absolutePath)
        } else {
            log("WARNING: Game jar not found at $gameJar")
        }

        command.add("-cp")
        command.add(classpath.joinToString(File.pathSeparator))

        // Main class
        command.add("net.minecraft.client.main.Main")

        // Game arguments
        command.add("--workDir=${instance.gameDir}")
        command.add("--launcherName=kokylaucher")
        command.add("--launcherVersion=1.1.0")

        if (version != null) {
            command.add("--version=${version.id}")
        } else {
            command.add("--version=${instance.versionId}")
        }

        command.add("--assetsDir=${File(instance.gameDir + "/assets").absolutePath}")
        command.add("--assetIndex=legacy")
        command.add("--uuid=${instance.id}")
        command.add("--userType=mojang")
        command.add("--accessToken=offline")
        command.add("--userType=legacy")

        return command
    }

    /**
     * Executes the launch command and captures output.
     */
    private fun executeLaunch(command: List<String>): LaunchResult {
        log("Executing: ${command.joinToString(" ")}")

        return try {
            val processBuilder = ProcessBuilder(*command.toTypedArray())
            processBuilder.directory(context.filesDir)
            processBuilder.redirectErrorStream(true)

            val process = processBuilder.start()
            log("Process started")

            // Read output in background
            val outputBuilder = StringBuilder()
            val readerThread = Thread {
                process.inputStream.bufferedReader().use { reader ->
                    reader.forEachLine { line ->
                        outputBuilder.append(line).append("\n")
                        log(line)
                    }
                }
            }
            readerThread.isDaemon = true
            readerThread.start()

            // Wait with timeout
            val finished = process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return LaunchResult.Failure(
                    IOException("Launch timed out after 60 seconds"),
                    getLaunchLog()
                )
            }

            val exitCode = process.exitValue()
            if (exitCode == 0) {
                log("Process exited with code 0 (success)")
                LaunchResult.Success(0)
            } else {
                log("Process exited with code $exitCode")
                LaunchResult.Failure(
                    IOException("Minecraft process exited with code $exitCode"),
                    getLaunchLog()
                )
            }
        } catch (e: Exception) {
            log("ERROR: ${e.message}")
            LaunchResult.Failure(e, getLaunchLog())
        }
    }

    /**
     * Returns the Java runtime directory.
     */
    fun getJavaHome(): File = javaHome

    /**
     * Returns the LWJGL directory.
     */
    fun getLwjglDir(): File = lwjglDir
}
