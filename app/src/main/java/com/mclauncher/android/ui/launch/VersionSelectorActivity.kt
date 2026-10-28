package com.mclauncher.android.ui.launch

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mclauncher.android.R
import com.mclauncher.android.data.model.Version
import com.mclauncher.android.data.repository.ModloaderRepository
import com.mclauncher.android.data.repository.InstanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.Result

class VersionSelectorActivity : AppCompatActivity() {

    private lateinit var modloaderRepository: ModloaderRepository
    private lateinit var recyclerView: RecyclerView
    private lateinit var versionAdapter: VersionAdapter
    private lateinit var versions: MutableList<Version>

    private lateinit var layoutLoading: View
    private lateinit var layoutError: View
    private lateinit var layoutEmpty: View
    private lateinit var btnRetry: Button
    private lateinit var tvErrorMessage: TextView
    private lateinit var etSearch: EditText

    private var selectedVersionId: String? = null

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_version_selector)

        modloaderRepository = ModloaderRepository(this)

        // Top bar
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // Search field
        etSearch = findViewById(R.id.etSearch)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterVersions(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Layout references
        layoutLoading = findViewById(R.id.layoutLoading)
        layoutError = findViewById(R.id.layoutError)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        btnRetry = findViewById(R.id.btnRetry)
        tvErrorMessage = findViewById(R.id.tvErrorMessage)

        btnRetry.setOnClickListener { loadVersions() }

        // RecyclerView
        recyclerView = findViewById(R.id.rvVersions)
        recyclerView.layoutManager = LinearLayoutManager(this)
        versions = mutableListOf()
        versionAdapter = VersionAdapter(versions) { version ->
            selectedVersionId = version.id
            versionAdapter.notifyDataSetChanged()
            // Save selection
            modloaderRepository.saveSelectedVersion(version.id, version.type)
            Toast.makeText(this, getString(R.string.version_selected_toast, version.id), Toast.LENGTH_SHORT).show()
        }
        recyclerView.adapter = versionAdapter

        // Load versions
        loadVersions()
    }

    private fun loadVersions() {
        layoutLoading.visibility = View.VISIBLE
        layoutError.visibility = View.GONE
        layoutEmpty.visibility = View.GONE
        recyclerView.visibility = View.GONE

        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                val result = fetchMojangVersions()
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        val versionList = result.getOrThrow()
                        versions.clear()
                        versions.addAll(versionList)
                        versionAdapter.notifyDataSetChanged()

                        if (versionList.isEmpty()) {
                            layoutEmpty.visibility = View.VISIBLE
                        } else {
                            recyclerView.visibility = View.VISIBLE
                        }
                        layoutLoading.visibility = View.GONE
                    } else {
                        layoutLoading.visibility = View.GONE
                        layoutError.visibility = View.VISIBLE
                        tvErrorMessage.text = "Chyba při načítání verzí: ${result.exceptionOrNull()?.message}"
                    }
                }
            }
        }
    }

    private fun filterVersions(query: String) {
        val lowerQuery = query.lowercase()
        val filtered = versions.filter { v ->
            v.id.lowercase().contains(lowerQuery) ||
            v.type.lowercase().contains(lowerQuery)
        }
        versionAdapter.submitList(filtered.toMutableList())
    }

    private suspend fun fetchMojangVersions(): Result<List<Version>> {
        return runCatching {
            var connection: HttpURLConnection? = null
            try {
                val url = URL("https://launchermeta.mojang.com/mc/game/version_manifest.json")
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.addRequestProperty("User-Agent", "Kokylaucher/1.2.0")

                val responseCode = connection.responseCode
                if (responseCode != 200) {
                    throw Exception("HTTP $responseCode: ${connection.responseMessage}")
                }

                val reader = java.io.InputStreamReader(connection.inputStream, java.nio.charset.StandardCharsets.UTF_8)
                val builder = StringBuilder()
                val buffer = CharArray(4096)
                var read: Int
                while (reader.read(buffer).also { read = it } != -1) {
                    builder.append(buffer, 0, read)
                }
                reader.close()

                val root = org.json.JSONObject(builder.toString())
                val versionsArray = root.getJSONArray("versions")
                val versions = mutableListOf<Version>()
                for (i in 0 until versionsArray.length()) {
                    val v = versionsArray.getJSONObject(i)
                    versions.add(Version(
                        id = v.getString("id"),
                        type = v.getString("type"),
                        url = v.getString("url"),
                        time = v.optString("time", ""),
                        releaseTime = v.optString("releaseTime", "")
                    ))
                }
                versions
            } finally {
                connection?.disconnect()
            }
        }
    }

    private inner class VersionAdapter(
        private var items: MutableList<Version>,
        private val onSelect: (Version) -> Unit
    ) : RecyclerView.Adapter<VersionAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvVersionId: TextView = itemView.findViewById(R.id.tvVersionId)
            val tvVersionType: TextView = itemView.findViewById(R.id.tvVersionType)
            val tvVersionDate: TextView = itemView.findViewById(R.id.tvVersionDate)
            val cbSelect: CheckBox = itemView.findViewById(R.id.cbSelect)
            val tvInstallStatus: TextView = itemView.findViewById(R.id.tvInstallStatus)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_version, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val version = items[position]
            holder.tvVersionId.text = version.id
            holder.tvVersionType.text = version.type.uppercase()
            holder.tvVersionDate.text = if (version.time.isNotEmpty()) version.time.substringBefore("T") else "neznámé"
            holder.cbSelect.isChecked = selectedVersionId == version.id

            // Check if installed
            val isInstalled = isVersionInstalled(version.id)
            holder.tvInstallStatus.text = if (isInstalled) getString(R.string.version_installed) else getString(R.string.version_not_installed)
            holder.tvInstallStatus.setTextColor(
                if (isInstalled) android.graphics.Color.parseColor("#FF34D399")
                else android.graphics.Color.parseColor("#FF666666")
            )

            holder.itemView.setOnClickListener {
                selectedVersionId = version.id
                notifyDataSetChanged()
                onSelect(version)
            }
        }

        override fun getItemCount(): Int = items.size

        fun submitList(newList: MutableList<Version>) {
            items.clear()
            items.addAll(newList)
            notifyDataSetChanged()
        }
    }

    private fun isVersionInstalled(versionId: String): Boolean {
        val instanceRepo = InstanceRepository(this)
        val instancesRoot = instanceRepo.getInstancesRoot()
        val versionFile = File(instancesRoot, "versions/$versionId.jar")
        return versionFile.exists()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
