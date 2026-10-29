package com.mclauncher.android.ui.settings

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mclauncher.android.R
import com.mclauncher.android.databinding.ActivitySettingsBinding
import com.mclauncher.android.util.LocaleHelper

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back button
        binding.btnBack.setOnClickListener { finish() }

        // Update app version text
        val versionName = buildConfigVersionName()
        binding.tvAppVersion.text = "Version $versionName"

        // Language button
        binding.btnLanguage.setOnClickListener {
            val intent = Intent(this, LanguageSettingsActivity::class.java)
            startActivity(intent)
        }

        // Runtime status button
        binding.btnRuntimeStatus.setOnClickListener {
            showRuntimeStatus()
        }

        // About button
        binding.btnAbout.setOnClickListener {
            showAbout()
        }

        // Licenses button
        binding.btnLicenses.setOnClickListener {
            showLicenses()
        }
    }

    private fun buildConfigVersionName(): String {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.3.0"
        } catch (e: Exception) {
            "1.3.0"
        }
    }

    private fun showRuntimeStatus() {
        val javaHome = java.io.File(filesDir, "java-runtime")
        val lwjglDir = java.io.File(filesDir, "lwjgl-android")
        val javaAvailable = javaHome.exists() && java.io.File(javaHome, "bin/java").canExecute()
        val lwjglAvailable = lwjglDir.exists() && lwjglDir.listFiles()?.any { it.extension == "so" } == true

        val message = StringBuilder()
        message.append("Java Runtime: ").append(if (javaAvailable) "Available" else "Not found").append("\n")
        message.append("LWJGL Natives: ").append(if (lwjglAvailable) "Available" else "Not found").append("\n")
        message.append("\nIf missing, Minecraft cannot be launched. See README for details.")

        android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.settings_runtime_status))
            .setMessage(message.toString())
            .setPositiveButton("OK") { _, _ -> }
            .show()
    }

    private fun showAbout() {
        val versionName = buildConfigVersionName()
        val message = getString(R.string.about_description) + "\n\n" +
            getString(R.string.about_version, versionName) + "\n" +
            getString(R.string.about_package)

        android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.settings_about))
            .setMessage(message)
            .setPositiveButton("OK") { _, _ -> }
            .show()
    }

    private fun showLicenses() {
        val licenses = getString(R.string.licenses_content)

        android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.settings_licenses))
            .setMessage(licenses)
            .setPositiveButton("OK") { _, _ -> }
            .show()
    }
}
