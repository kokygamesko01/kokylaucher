package com.mclauncher.android.ui.versions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.mclauncher.android.R
import com.mclauncher.android.data.model.Version
import com.mclauncher.android.data.repository.VersionRepository
import kotlinx.coroutines.launch

class VersionsFragment : Fragment() {

    private lateinit var versionRepository: VersionRepository
    private lateinit var versionsList: ListView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var refreshButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_versions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        versionRepository = VersionRepository(requireContext())

        versionsList = view.findViewById(R.id.versions_list)
        progressBar = view.findViewById(R.id.progress_bar)
        errorText = view.findViewById(R.id.error_text)
        refreshButton = view.findViewById(R.id.refresh_button)

        refreshButton.setOnClickListener { loadVersions() }

        loadVersions()
    }

    private fun loadVersions() {
        progressBar.visibility = View.VISIBLE
        errorText.visibility = View.GONE
        versionsList.visibility = View.GONE

        lifecycleScope.launch {
            val result = versionRepository.fetchVersions()
            progressBar.visibility = View.GONE

            if (result.isSuccess) {
                val versions = result.getOrNull() ?: emptyList()
                showVersions(versions)
            } else {
                errorText.text = "Failed to load versions: ${result.exceptionOrNull()?.message}"
                errorText.visibility = View.VISIBLE
            }
        }
    }

    private fun showVersions(versions: List<Version>) {
        // Separate releases from snapshots
        val releases = versions.filter { it.type == "release" }
        val snapshots = versions.filter { it.type == "snapshot" }

        val items = mutableListOf<String>()
        if (releases.isNotEmpty()) {
            items.add("=== Releases ===")
            releases.forEach { items.add("${it.id} (${it.type})") }
        }
        if (snapshots.isNotEmpty()) {
            items.add("=== Snapshots ===")
            snapshots.forEach { items.add("${it.id} (${it.type})") }
        }

        val adapter = ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_list_item_1,
            items
        )
        versionsList.adapter = adapter
        versionsList.visibility = View.VISIBLE

        versionsList.setOnItemClickListener { _, _, position, _ ->
            val selected = items[position]
            if (selected.startsWith("===")) return@setOnItemClickListener
            val versionId = selected.substringBefore(" (")
            Toast.makeText(requireContext(), "Selected: $versionId", Toast.LENGTH_SHORT).show()
        }
    }
}
