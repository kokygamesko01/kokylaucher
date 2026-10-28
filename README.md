# MC Launcher

A Minecraft: Java Edition launcher manager for Android. Download Minecraft versions, create isolated game instances, install mods and resource packs, view logs, and manage backups — all from your Android phone.

## ⚠️ Important Limitation: Game Launching

Minecraft: Java Edition cannot run natively on Android because:

1. **Native libraries:** Minecraft's native dependencies (LWJGL, OpenAL, etc.) are compiled for desktop operating systems (Windows/Linux/macOS on x86/x64), not Android ARM64.
2. **JVM:** Android uses ART (Android Runtime), not a standard Java Virtual Machine. The Minecraft client JAR requires a JVM to execute.

This app is a **launcher manager** — it downloads versions, libraries, assets, mods, and resource packs into isolated instances and assembles the Java launch command. Actually executing the game on Android requires a JVM port plus Android-native LWJGL builds, as implemented by the open-source [PojavLauncher](https://github.com/TeamPojavLauncher/PojavLauncher) (GPL-3.0). This project studied PojavLauncher and PrismLauncher/MultiMC for architectural reference only; no code was copied.

## Features

- **Version Management:** Fetch and browse Minecraft versions from Mojang's official version manifest (releases and snapshots)
- **Isolated Instances:** Create multiple game instances, each with its own directory containing mods/, resourcepacks/, saves/, config/, logs/, and more
- **Mod Support:** Framework for installing Fabric, Forge, or Quilt mods (mods/ directory per instance)
- **Resource Packs:** Resource packs directory per instance
- **Logs:** View instance log files
- **Backups:** Create ZIP backups of instances and restore from them
- **Settings:** App settings and about information
- **Modern UI:** Material Design 3 interface with Bottom Navigation, light/dark theme support

## Permissions

| Permission | Purpose |
|------------|---------|
| `INTERNET` | Download version manifest, libraries, assets from Mojang servers |
| `ACCESS_NETWORK_STATE` | Check network connectivity before downloads |
| `WAKE_LOCK` | Keep device awake during downloads and operations |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_DATA_SYNC` | Run long-running backup/download operations in the background |
| `POST_NOTIFICATIONS` | Show download progress and operation status (Android 13+) |

No `WRITE_EXTERNAL_STORAGE` or `READ_EXTERNAL_STORAGE` is required — instances are stored in the app's private external files directory.

## Dependencies

All dependencies are permissively licensed (Apache-2.0 or MIT-compatible):

| Dependency | License | Purpose |
|------------|---------|---------|
| Kotlin stdlib | Apache 2.0 | Kotlin standard library |
| AndroidX AppCompat | Apache 2.0 | Backward-compatible UI components |
| AndroidX Core KTX | Apache 2.0 | Kotlin extensions for AndroidX |
| Material Components | Apache 2.0 | Material Design 3 UI components |
| OkHttp | Apache 2.0 | HTTP client (declared, not yet used) |
| Gson | Apache 2.0 | JSON parsing (declared, not yet used) |
| kotlinx-coroutines | Apache 2.0 | Async/background tasks |

## Project References (study only, no code copied)

- [PojavLauncher](https://github.com/TeamPojavLauncher/PojavLauncher) (GPL-3.0) — Minecraft Java Edition on Android via JVM port + Android-native LWJGL
- [PrismLauncher](https://github.com/PrismLauncher/PrismLauncher) (GPL-3.0) — MultiMC-style instance management

## Building

```bash
# Clone the repository
git clone https://github.com/your-username/MCLauncher.git
cd MCLauncher

# Build debug APK
./gradlew assembleDebug

# The APK will be at: app/build/outputs/apk/debug/app-debug.apk
```

## License

MIT License. See [LICENSE](LICENSE) for details.

## Architecture Reference

This project was designed with reference to the following open-source projects (architectural study only; no code was copied due to GPL-3.0 licensing):

- **PojavLauncher** — JVM port strategy for running Java Edition on Android
- **PrismLauncher/MultiMC** — Instance isolation, version JSON parsing, launch command assembly
