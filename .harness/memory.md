AndroidHarness local build toolchain (verified working):
- Java 21.0.13 (openjdk-21), JAVA_HOME=/data/data/com.androidharness.app/files/linux/lib/jvm/java-21-openjdk (java/javac on PATH)
- Gradle 9.8.2 — requires GRADLE_USER_HOME and TMPDIR set to $HARNESS_SCRATCH (/data/user/0/com.androidharness.app/files/.harness-scratch/...) else libnative-platform.so extraction fails on bionic userspace
- Android SDK at $HARNESS_SCRATCH/android-sdk: cmdline-tools 12.0, build-tools 34.0.0 (aapt2, d8, zipalign, apksigner), platform android-34, platform-tools (adb 37.0.1)
- SDK licenses accepted via sdkmanager --licenses
- Network OK: dl.google.com, api.github.com, maven.google.com reachable
- Workspace: /storage/emulated/0/Android/data/com.androidharness.app/files/workspace (empty, git not yet initialized)
- Shell blocks exporting paths under /data/data/com.androidharness.app/files/linux (JAVA_HOME), but HARNESS_SCRATCH paths allowed; java on PATH so JAVA_HOME not needed for gradle/sdkmanager
Key architectural finding: Minecraft Java Edition natives (LWJGL/OpenAL) are desktop x86/x64 and Android uses ART not a JVM — an SDK-only launcher cannot RUN Java Edition; needs JVM port + Android-native LWJGL (PojavLauncher approach, GPL-3.0, reference only). App will be a full launcher MANAGER (versions, isolated instances, mods, resource packs, settings, logs, backups, launch-command generation) with the game-launch limitation clearly documented. Reference projects: PojavLauncher (GPL-3.0), PrismLauncher/MultiMC (GPL-3.0) — studied for algorithms only, no code copied.
Dependencies (all Apache-2.0 / MIT-compatible): Kotlin stdlib, Jetpack Compose/Material3, OkHttp, Gson, kotlinx-coroutines, AndroidX Room/DataStore/WorkManager.
