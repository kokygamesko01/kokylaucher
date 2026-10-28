package com.mclauncher.android.ui.launch

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mclauncher.android.R
import com.mclauncher.android.data.model.LoaderVersion
import com.mclauncher.android.data.model.Modloader
import com.mclauncher.android.data.repository.ModloaderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.Result

class ModloaderSelectorActivity : AppCompatActivity() {

    private lateinit var modloaderRepository: ModloaderRepository

    private lateinit var layoutModloaders: ViewGroup
    private lateinit var tvSelectedMcVersion: TextView
    private lateinit var tvCompatHint: TextView
    private lateinit var btnCancel: Button
    private lateinit var btnSelectLoader: Button

    private val defaultModloaders: List<Modloader> = mutableListOf()
    private var selectedModloaderId: String? = null
    private var selectedLoaderVersion: LoaderVersion? = null
    private var selectedMcVersion: String? = null
    private var selectedMcType: String? = null

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modloader_selector)

        modloaderRepository = ModloaderRepository(this)

        // Top bar
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // Layout references
        layoutModloaders = findViewById(R.id.layoutModloaders)
        tvSelectedMcVersion = findViewById(R.id.tvSelectedMcVersion)
        tvCompatHint = findViewById(R.id.tvCompatHint)
        btnCancel = findViewById(R.id.btnCancel)
        btnSelectLoader = findViewById(R.id.btnSelectLoader)

        btnCancel.setOnClickListener { finish() }
        btnSelectLoader.setOnClickListener {
            saveAndFinish()
        }

        // Get selected Minecraft version from intent or prefs
        val intentMcVersion = intent.getStringExtra("minecraft_version")
        selectedMcType = intent.getStringExtra("minecraft_type")

        selectedMcVersion = intentMcVersion
        val mcVersionCheck = selectedMcVersion
        if (mcVersionCheck == null || mcVersionCheck.isEmpty()) {
            selectedMcVersion = modloaderRepository.getSelectedVersionId()
            selectedMcType = modloaderRepository.getSelectedVersionType()
        }

        // Use local val for smart cast
        val mcVersion = selectedMcVersion

        // Display selected MC version
        val displayText = if (mcVersion != null) {
            "$mcVersion (${selectedMcType ?: "release"})"
        } else {
            "Není vybrána"
        }
        tvSelectedMcVersion.text = displayText

        if (mcVersion == null) {
            tvCompatHint.text = getString(R.string.modloader_no_mc)
            btnSelectLoader.isEnabled = false
        } else {
            tvCompatHint.text = getString(R.string.modloader_compat_hint).replace("%s", mcVersion)
            loadModloaders(mcVersion)
        }
    }

    private fun loadModloaders(mcVersion: String) {
        layoutModloaders.removeAllViews()

        val modloaders = modloaderRepository.getDefaultModloaders()

        for (modloader in modloaders) {
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_modloader, layoutModloaders, false)
            layoutModloaders.addView(itemView)

            val tvName: TextView = itemView.findViewById(R.id.tvLoaderName)
            val tvDesc: TextView = itemView.findViewById(R.id.tvLoaderDescription)
            val rbSelect: RadioButton = itemView.findViewById(R.id.rbSelect)
            val layoutVersions: ViewGroup = itemView.findViewById(R.id.layoutVersions)

            tvName.text = modloader.name
            tvDesc.text = modloader.description

            // Get compatible loader versions for this MC version
            val compatibleVersions = getCompatibleLoaderVersions(modloader.id, mcVersion)

            if (compatibleVersions.isEmpty()) {
                // No compatible versions - show disabled
                tvDesc.text = getString(R.string.modloader_no_compatible).replace("%s", mcVersion)
                rbSelect.isEnabled = false
                rbSelect.setTextColor(android.graphics.Color.parseColor("#FF666666"))
                tvName.setTextColor(android.graphics.Color.parseColor("#FF666666"))

                val tvNoCompat = TextView(this)
                tvNoCompat.text = getString(R.string.modloader_no_compat)
                tvNoCompat.setTextColor(android.graphics.Color.parseColor("#FFEF4444"))
                tvNoCompat.textSize = 12f
                layoutVersions.addView(tvNoCompat)
            } else {
                // Add version radio buttons
                for (lv in compatibleVersions) {
                    val versionItem = LayoutInflater.from(this).inflate(R.layout.item_loader_version, layoutVersions, false)
                    val rbVersion: RadioButton = versionItem.findViewById(R.id.rbLoaderVersion)
                    val tvStable: TextView = versionItem.findViewById(R.id.tvLoaderVersionStable)

                    rbVersion.text = lv.version
                    tvStable.visibility = if (lv.stable) View.VISIBLE else View.GONE

                    rbVersion.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            selectedModloaderId = modloader.id
                            selectedLoaderVersion = lv
                            rbSelect.isChecked = true
                            btnSelectLoader.isEnabled = true
                            // Unselect other modloader radio buttons
                            for (i in 0 until layoutModloaders.childCount) {
                                val child = layoutModloaders.getChildAt(i)
                                val otherRb = child.findViewById<RadioButton>(R.id.rbSelect)
                                if (otherRb != null && otherRb != rbSelect) {
                                    otherRb.isChecked = false
                                }
                            }
                        }
                    }

                    layoutVersions.addView(versionItem)
                }
            }

            rbSelect.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedModloaderId = modloader.id
                    btnSelectLoader.isEnabled = selectedLoaderVersion != null
                    // Unselect other modloaders
                    for (i in 0 until layoutModloaders.childCount) {
                        val child = layoutModloaders.getChildAt(i)
                        val otherRb = child.findViewById<RadioButton>(R.id.rbSelect)
                        if (otherRb != null && otherRb != rbSelect) {
                            otherRb.isChecked = false
                        }
                    }
                    // Auto-select first compatible version if none selected
                    val compatible = getCompatibleLoaderVersions(modloader.id, mcVersion)
                    if (selectedLoaderVersion == null && compatible.isNotEmpty()) {
                        val firstVersionItem = layoutVersions.getChildAt(0)
                        val firstRb = firstVersionItem.findViewById<RadioButton>(R.id.rbLoaderVersion)
                        firstRb.isChecked = true
                    }
                }
            }

            itemView.setOnClickListener {
                rbSelect.isChecked = true
            }
        }
    }

    private fun getCompatibleLoaderVersions(modloaderId: String, mcVersion: String): List<LoaderVersion> {
        return when (modloaderId) {
            "fabric" -> {
                mutableListOf<LoaderVersion>().also { list ->
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            val result = modloaderRepository.fetchFabricLoaderVersions(mcVersion)
                            withContext(Dispatchers.Main) {
                                if (result.isSuccess) {
                                    list.addAll(result.getOrThrow())
                                } else {
                                    // Fallback to known versions
                                    val knownVersion = modloaderRepository.getKnownQuiltVersion(mcVersion)
                                    if (knownVersion != null) {
                                        list.add(LoaderVersion(knownVersion, mcVersion, stable = true))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "forge" -> {
                val known = modloaderRepository.getKnownForgeVersion(mcVersion)
                if (known != null) {
                    listOf(LoaderVersion(known, mcVersion, stable = true))
                } else {
                    emptyList()
                }
            }
            "neoforge" -> {
                val known = modloaderRepository.getKnownNeoForgeVersion(mcVersion)
                if (known != null) {
                    listOf(LoaderVersion(known, mcVersion, stable = true))
                } else {
                    emptyList()
                }
            }
            "quilt" -> {
                val known = modloaderRepository.getKnownQuiltVersion(mcVersion)
                if (known != null) {
                    listOf(LoaderVersion(known, mcVersion, stable = true))
                } else {
                    emptyList()
                }
            }
            else -> emptyList()
        }
    }

    private fun saveAndFinish() {
        if (selectedModloaderId == null || selectedLoaderVersion == null) {
            Toast.makeText(this, getString(R.string.modloader_select_both), Toast.LENGTH_SHORT).show()
            return
        }

        modloaderRepository.saveSelectedModloader(
            selectedModloaderId,
            selectedLoaderVersion!!.version,
            selectedLoaderVersion!!.minecraftVersion
        )

        Toast.makeText(
            this,
            getString(R.string.modloader_select_toast, selectedModloaderId, selectedLoaderVersion!!.version),
            Toast.LENGTH_SHORT
        ).show()

        val resultIntent = Intent().apply {
            putExtra("selected_modloader", selectedModloaderId)
            putExtra("selected_loader_version", selectedLoaderVersion!!.version)
            putExtra("selected_loader_mcversion", selectedLoaderVersion!!.minecraftVersion)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}
