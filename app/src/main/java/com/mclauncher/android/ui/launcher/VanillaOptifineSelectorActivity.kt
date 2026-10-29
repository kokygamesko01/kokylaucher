package com.mclauncher.android.ui.launcher

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mclauncher.android.R
import com.mclauncher.android.databinding.ActivityVanillaOptifineSelectorBinding
import com.mclauncher.android.data.repository.ModloaderRepository
import com.mclauncher.android.ui.launch.VersionSelectorActivity

class VanillaOptifineSelectorActivity : AppCompatActivity() {

    private lateinit var modloaderRepository: ModloaderRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vanilla_optifine_selector)

        modloaderRepository = ModloaderRepository(this)

        // Back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // Vanilla version selection
        findViewById<Button>(R.id.btnSelectVanillaVersion).setOnClickListener {
            val intent = Intent(this, VersionSelectorActivity::class.java)
            startActivityForResult(intent, 1003)
        }

        // OptiFine version selection
        findViewById<Button>(R.id.btnSelectOptifineVersion).setOnClickListener {
            showOptifineSelector()
        }

        // Update display
        updateDisplay()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1003 && resultCode == RESULT_OK) {
            val versionId = data?.getStringExtra("selected_version_id")
            val versionType = data?.getStringExtra("selected_version_type")
            if (versionId != null) {
                modloaderRepository.saveSelectedVersion(versionId, versionType ?: "release")
                Toast.makeText(this, "Vanilla version $versionId selected", Toast.LENGTH_SHORT).show()
                updateDisplay()
            }
        }
    }

    private fun updateDisplay() {
        val prefs = getSharedPreferences("kokylaucher_prefs", MODE_PRIVATE)
        val versionId = prefs.getString("selected_version_id", null)
        val versionType = prefs.getString("selected_version_type", null)

        findViewById<TextView>(R.id.tvVanillaSelected).text = if (versionId != null) {
            "$versionId ($versionType)"
        } else {
            "No version selected"
        }

        findViewById<TextView>(R.id.tvOptifineSelected).text = "No version selected (OptiFine requires manual download from optifine.net)"
    }

    private fun showOptifineSelector() {
        AlertDialog.Builder(this)
            .setTitle("OptiFine Version")
            .setMessage("OptiFine is not automatically distributed through Mojang's manifest.\n\nTo use OptiFine:\n1. Download the OptiFine installer from optifine.net\n2. Place the OptiFine JAR in your instance's mods/ folder\n3. Select the matching Minecraft version in Vanilla mode\n\nOptiFine downloads are subject to OptiFine distribution terms.")
            .setPositiveButton("OK") { _, _ -> }
            .show()
    }
}
