package com.mclauncher.android

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mclauncher.android.databinding.ActivityMainBinding
import com.mclauncher.android.service.LaunchManager
import com.mclauncher.android.ui.launch.LaunchActivity
import com.mclauncher.android.ui.launch.VersionSelectorActivity
import com.mclauncher.android.ui.launcher.ModLauncherSelectorActivity
import com.mclauncher.android.ui.launcher.VanillaOptifineSelectorActivity
import com.mclauncher.android.ui.settings.SettingsActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var launchManager: LaunchManager

    // Request codes for selector activities
    private val REQUEST_VERSION_SELECTOR = 1001
    private val REQUEST_MODLOADER_SELECTOR = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        launchManager = LaunchManager(this)

        // Load persisted nickname
        val prefs = getSharedPreferences("kokylaucher_prefs", MODE_PRIVATE)
        val playerName = prefs.getString("player_name", null) ?: "Player"
        binding.tvWelcomeName.text = "Welcome, $playerName"

        // Home button
        binding.btnHome.setOnClickListener {
            binding.tvWelcomeName.text = "Welcome, $playerName"
        }

        // Settings button
        binding.btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // Vanilla Launcher card - opens Vanilla/OptiFine selector
        binding.root.findViewById<View>(com.mclauncher.android.R.id.cardVanilla)?.setOnClickListener {
            val intent = Intent(this, VanillaOptifineSelectorActivity::class.java)
            startActivity(intent)
        }

        // Mod Launcher card - opens Mod Launcher selector
        binding.root.findViewById<View>(com.mclauncher.android.R.id.cardMod)?.setOnClickListener {
            val savedVersionId = prefs.getString("selected_version_id", null)
            if (savedVersionId == null || savedVersionId.isEmpty()) {
                Toast.makeText(this, getString(R.string.no_version_selected), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, ModLauncherSelectorActivity::class.java)
            startActivity(intent)
        }

        // Utility buttons
        binding.btnShareLog.setOnClickListener {
            shareLog()
        }

        binding.btnOpenGameFolder.setOnClickListener {
            openGameFolder()
        }

        // Play button
        binding.btnPlay.setOnClickListener {
            attemptLaunch()
        }

        // Update selection display
        updateSelectionDisplay()
    }

    override fun onResume() {
        super.onResume()
        updateSelectionDisplay()
    }

    private fun updateSelectionDisplay() {
        val prefs = getSharedPreferences("kokylaucher_prefs", MODE_PRIVATE)
        val versionId = prefs.getString("selected_version_id", null)
        val versionType = prefs.getString("selected_version_type", null)
        val modloaderId = prefs.getString("selected_modloader", null)
        val loaderVersion = prefs.getString("selected_loader_version", null)

        // Update Vanilla card version
        binding.tvVanillaVersion.text = if (versionId != null) {
            "$versionId ($versionType)"
        } else {
            getString(R.string.no_version_selected)
        }

        // Update Mod card loader
        binding.tvModLoader.text = if (modloaderId != null && loaderVersion != null) {
            "$modloaderId • $loaderVersion"
        } else {
            getString(R.string.no_modloader_selected)
        }
    }

    private fun shareLog() {
        val log = launchManager.getLaunchLog()
        if (log.isEmpty()) {
            Toast.makeText(this, "No logs available", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Kokylaucher Launch Log")
            putExtra(Intent.EXTRA_TEXT, log)
        }
        startActivity(Intent.createChooser(intent, "Share log"))
    }

    private fun openGameFolder() {
        val instancesRoot = launchManagerInstanceRepository().getInstancesRoot()
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(Uri.fromFile(instancesRoot), "*/*")
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open game folder: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun attemptLaunch() {
        val prefs = getSharedPreferences("kokylaucher_prefs", MODE_PRIVATE)
        val versionId = prefs.getString("selected_version_id", null)
        val modloader = prefs.getString("selected_modloader", null)
        val loaderVersion = prefs.getString("selected_loader_version", null)

        if (versionId == null || versionId.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.no_version_selected))
                .setMessage(getString(R.string.no_version_message))
                .setPositiveButton("OK") { _, _ -> }
                .show()
            return
        }

        if (!launchManager.isJavaRuntimeAvailable()) {
            showRuntimeMissingDialog()
            return
        }

        if (!launchManager.isLwjglAvailable()) {
            showLwjglMissingDialog()
            return
        }

        val readiness = launchManager.checkLaunchReadiness(versionId, modloader, loaderVersion)
        if (!readiness.isReady) {
            showMissingFilesDialog(readiness)
            return
        }

        val intent = Intent(this, LaunchActivity::class.java).apply {
            putExtra("instance_id", versionId)
            putExtra("version_id", versionId)
            putExtra("modloader", modloader)
            putExtra("loader_version", loaderVersion)
        }
        startActivity(intent)
    }

    private fun showRuntimeMissingDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.runtime_not_available))
            .setMessage(getString(R.string.play_launch_error_detail))
            .setPositiveButton("OK") { _, _ -> }
            .show()
    }

    private fun showLwjglMissingDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.lwjgl_not_available))
            .setMessage("Android-native LWJGL libraries are not available.\n\n" +
                "Minecraft uses LWJGL for graphics, sound, and input. These libraries are built for desktop (Windows/Linux/macOS x86/x64) and cannot be loaded on Android ARM64.\n\n" +
                "For real execution, Android-native LWJGL (built for ARM64) and Java runtime for Android are required.")
            .setPositiveButton("OK") { _, _ -> }
            .show()
    }

    private fun showMissingFilesDialog(readiness: com.mclauncher.android.data.model.LaunchReadiness) {
        val missing = readiness.missingComponents.joinToString("\n") { component: String ->
            "• $component"
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.missing_files_title))
            .setMessage(getString(R.string.missing_files_message).replace("%s", missing))
            .setPositiveButton("OK") { _, _ -> }
            .show()
    }

    private fun launchManagerInstanceRepository(): com.mclauncher.android.data.repository.InstanceRepository {
        return com.mclauncher.android.data.repository.InstanceRepository(this)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
