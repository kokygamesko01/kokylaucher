package com.mclauncher.android.ui.launcher

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mclauncher.android.R
import com.mclauncher.android.databinding.ActivityModLauncherSelectorBinding
import com.mclauncher.android.data.repository.ModloaderRepository
import com.mclauncher.android.ui.launch.ModloaderSelectorActivity

class ModLauncherSelectorActivity : AppCompatActivity() {

    private lateinit var modloaderRepository: ModloaderRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mod_launcher_selector)

        modloaderRepository = ModloaderRepository(this)

        // Back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // Fabric card
        findViewById<androidx.cardview.widget.CardView>(R.id.cardFabric).setOnClickListener {
            openModloaderSelector("fabric")
        }

        // Quilt card
        findViewById<androidx.cardview.widget.CardView>(R.id.cardQuilt).setOnClickListener {
            openModloaderSelector("quilt")
        }

        // Legacy Fabric card
        findViewById<androidx.cardview.widget.CardView>(R.id.cardLegacyFabric).setOnClickListener {
            openModloaderSelector("legacy_fabric")
        }

        // Forge card
        findViewById<androidx.cardview.widget.CardView>(R.id.cardForge).setOnClickListener {
            openModloaderSelector("forge")
        }

        // NeoForge card
        findViewById<androidx.cardview.widget.CardView>(R.id.cardNeoforge).setOnClickListener {
            openModloaderSelector("neoforge")
        }

        // Modpacks card (disabled)
        findViewById<androidx.cardview.widget.CardView>(R.id.cardModpacks).setOnClickListener {
            Toast.makeText(this, "Modpack installation workflow not yet available", Toast.LENGTH_SHORT).show()
        }

        updateDisplay()
    }

    private fun openModloaderSelector(modloaderId: String) {
        val prefs = getSharedPreferences("kokylaucher_prefs", MODE_PRIVATE)
        val savedVersionId = prefs.getString("selected_version_id", null)

        if (savedVersionId == null || savedVersionId.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Select Minecraft Version First")
                .setMessage("Please select a Minecraft version in the Vanilla Launcher before choosing a modloader.")
                .setPositiveButton("OK") { _, _ -> }
                .show()
            return
        }

        val intent = Intent(this, ModLauncherSelectorActivity::class.java).apply {
            putExtra("minecraft_version", savedVersionId)
            putExtra("minecraft_type", prefs.getString("selected_version_type", null))
        }
        startActivityForResult(intent, 1004)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1004 && resultCode == RESULT_OK) {
            val modloaderId = data?.getStringExtra("selected_modloader") ?: ""
            val loaderVersion = data?.getStringExtra("selected_loader_version") ?: ""
            Toast.makeText(this, "$modloaderId $loaderVersion selected", Toast.LENGTH_SHORT).show()
            updateDisplay()
        }
    }

    private fun updateDisplay() {
        val prefs = getSharedPreferences("kokylaucher_prefs", MODE_PRIVATE)
        val modloaderId = prefs.getString("selected_modloader", null)
        val loaderVersion = prefs.getString("selected_loader_version", null)

        // Update version displays for each modloader card
        // These show the selected version per modloader type
        // For simplicity, all modloaders share the same selected version from prefs
        val sharedVersionId = prefs.getString("selected_version_id", null)

        findViewById<TextView>(R.id.tvFabricVersion).text = if (modloaderId == "fabric" && sharedVersionId != null) {
            sharedVersionId
        } else if (modloaderId == "fabric") {
            "Select a Minecraft version first"
        } else {
            "Not selected"
        }

        findViewById<TextView>(R.id.tvQuiltVersion).text = if (modloaderId == "quilt" && sharedVersionId != null) {
            sharedVersionId
        } else if (modloaderId == "quilt") {
            "Select a Minecraft version first"
        } else {
            "Not selected"
        }

        findViewById<TextView>(R.id.tvLegacyFabricVersion).text = if (modloaderId == "legacy_fabric" && sharedVersionId != null) {
            sharedVersionId
        } else if (modloaderId == "legacy_fabric") {
            "Select a Minecraft version first"
        } else {
            "Not selected"
        }

        findViewById<TextView>(R.id.tvForgeVersion).text = if (modloaderId == "forge" && sharedVersionId != null) {
            sharedVersionId
        } else if (modloaderId == "forge") {
            "Select a Minecraft version first"
        } else {
            "Not selected"
        }

        findViewById<TextView>(R.id.tvNeoforgeVersion).text = if (modloaderId == "neoforge" && sharedVersionId != null) {
            sharedVersionId
        } else if (modloaderId == "neoforge") {
            "Select a Minecraft version first"
        } else {
            "Not selected"
        }
    }
}
