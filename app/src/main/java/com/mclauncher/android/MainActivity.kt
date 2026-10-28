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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var playerName: String = "Hráč"
    private var launcherType: String = "vanilla" // "vanilla" or "mod"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load persisted nickname, fall back to intent extra, then default
        val prefs = getSharedPreferences("kokylaucher_prefs", MODE_PRIVATE)
        playerName = prefs.getString("player_name", null)
            ?: intent.getStringExtra("player_name")
            ?: "Hráč"

        // Top bar - nickname
        binding.tvAppTitle.text = playerName

        binding.btnHome.setOnClickListener {
            // Home
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
            Toast.makeText(this, "Vlastní ovládací prvky - připraveno", Toast.LENGTH_SHORT).show()
        }

        binding.btnRunJar.setOnClickListener {
            Toast.makeText(this, "Spustit .JAR - připraveno", Toast.LENGTH_SHORT).show()
        }

        binding.btnShareLog.setOnClickListener {
            shareLog()
        }

        binding.btnOpenGameFolder.setOnClickListener {
            openGameFolder()
        }

        // Play button
        binding.btnPlay.setOnClickListener {
            showPlayDialog()
        }
    }

    private fun shareLog() {
        Toast.makeText(this, "Sdílet záznam protokolu - připraveno", Toast.LENGTH_SHORT).show()
    }

    private fun openGameFolder() {
        Toast.makeText(this, "Otevřít složku hry - připraveno", Toast.LENGTH_SHORT).show()
    }

    private fun showPlayDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.play_not_available))
            .setMessage(getString(R.string.play_explanation))
            .setPositiveButton("Rozumím") { _, _ ->
                // Dismiss
            }
            .setCancelable(false)
            .show()
    }
}
