package com.mclauncher.android

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mclauncher.android.databinding.ActivityMainBinding
import com.mclauncher.android.service.LaunchManager
import com.mclauncher.android.ui.launch.LaunchActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var launchManager: LaunchManager
    private var playerName: String = "Hráč"
    private var launcherType: String = "vanilla" // "vanilla" or "mod"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        launchManager = LaunchManager(this)

        playerName = intent.getStringExtra("player_name") ?: "Hráč"

        // Load persisted nickname, fall back to intent extra, then default
        val prefs = getSharedPreferences("kokylaucher_prefs", MODE_PRIVATE)
        playerName = prefs.getString("player_name", null)
            ?: intent.getStringExtra("player_name")
            ?: "Hráč"

        // Top bar - nickname
        binding.tvAppTitle.text = playerName

        binding.btnHome.setOnClickListener {
            // Home - refresh view
            binding.tvAppTitle.text = playerName
        }

        // Launcher type choice
        binding.btnVanilla.setOnClickListener {
            launcherType = "vanilla"
            binding.btnVanilla.setBackgroundResource(R.drawable.button_koky_selected)
            binding.btnMod.setBackgroundResource(R.drawable.button_koky)
            Toast.makeText(this, "Vybrán Vanilla Launcher", Toast.LENGTH_SHORT).show()
        }

        binding.btnMod.setOnClickListener {
            launcherType = "mod"
            binding.btnMod.setBackgroundResource(R.drawable.button_koky_selected)
            binding.btnVanilla.setBackgroundResource(R.drawable.button_koky)
            Toast.makeText(this, "Vybrán Mod Launcher", Toast.LENGTH_SHORT).show()
        }

        // Default to vanilla
        binding.btnVanilla.setBackgroundResource(R.drawable.button_koky_selected)

        // Menu items
        binding.btnCustomControls.setOnClickListener {
            showFeatureStatus("Vlastní ovládací prvky")
        }

        binding.btnRunJar.setOnClickListener {
            showFeatureStatus("Spustit .JAR")
        }

        binding.btnShareLog.setOnClickListener {
            shareLog()
        }

        binding.btnOpenGameFolder.setOnClickListener {
            openGameFolder()
        }

        // Play button - attempts genuine Minecraft launch
        binding.btnPlay.setOnClickListener {
            attemptLaunch()
        }
    }

    private fun showFeatureStatus(featureName: String) {
        AlertDialog.Builder(this)
            .setTitle(featureName)
            .setMessage("${getString(R.string.button_not_implemented)}\nTato funkce bude přidána v nadcházející aktualizaci.")
            .setPositiveButton("Rozumím") { _, _ -> }
            .show()
    }

    private fun shareLog() {
        val log = launchManager.getLaunchLog()
        if (log.isEmpty()) {
            Toast.makeText(this, "Žádné protokoly k dispozici", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Kokylaucher Launch Log")
            putExtra(Intent.EXTRA_TEXT, log)
        }
        startActivity(Intent.createChooser(intent, "Sdílet protokol"))
    }

    private fun openGameFolder() {
        val instancesRoot = launchManagerInstanceRepository().getInstancesRoot()
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(android.net.Uri.fromFile(instancesRoot), "*/*")
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Nelze otevřít složku her: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun attemptLaunch() {
        // Get the most recent instance
        val instances = launchManagerInstanceRepository().listInstances()
        if (instances.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Žádné instance")
                .setMessage("Nejprve vytvořte instanci hry v sekci Instances.")
                .setPositiveButton("Rozumím") { _, _ -> }
                .show()
            return
        }

        val instance = instances[0] // Use most recent instance

        // Check if runtime is available
        if (!launchManager.isJavaRuntimeAvailable()) {
            showRuntimeMissingDialog()
            return
        }

        if (!launchManager.isLwjglAvailable()) {
            showLwjglMissingDialog()
            return
        }

        // Launch via LaunchActivity
        val intent = Intent(this, LaunchActivity::class.java).apply {
            putExtra("instance_id", instance.id)
            putExtra("version_id", instance.versionId)
        }
        startActivity(intent)
    }

    private fun showRuntimeMissingDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.runtime_not_available))
            .setMessage(getString(R.string.play_launch_error_detail))
            .setPositiveButton("Rozumím") { _, _ -> }
            .show()
    }

    private fun showLwjglMissingDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.lwjgl_not_available))
            .setMessage("Android-native LWJGL knihovny nejsou k dispozici.\n\n" +
                "Minecraft používá LWJGL pro grafiku, zvuk a vstup. Tyto knihovny jsou sestaveny pro desktop (Windows/Linux/macOS x86/x64) a nelze je na Androidu ARM64 přímo načíst.\n\n" +
                "Pro reálné spuštění je nutné Android-native LWJGL (sestavené pro ARM64) a běhové prostředí Java pro Android.")
            .setPositiveButton("Rozumím") { _, _ -> }
            .show()
    }

    private fun launchManagerInstanceRepository(): com.mclauncher.android.data.repository.InstanceRepository {
        return com.mclauncher.android.data.repository.InstanceRepository(this)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
