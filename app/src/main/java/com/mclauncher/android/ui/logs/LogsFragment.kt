package com.mclauncher.android.ui.logs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.mclauncher.android.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment for viewing log files of a game instance.
 */
class LogsFragment : Fragment() {

    private lateinit var logText: TextView
    private lateinit var instanceName: TextView
    private lateinit var refreshButton: Button
    private var logDir: File? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_logs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logText = view.findViewById(R.id.log_text)
        instanceName = view.findViewById(R.id.instance_name)
        refreshButton = view.findViewById(R.id.refresh_button)

        // Get log directory from arguments
        val logDirPath = arguments?.getString("log_dir") ?: ""
        val name = arguments?.getString("instance_name") ?: "Logs"
        logDir = File(logDirPath)

        instanceName.text = "Logs — $name"

        refreshButton.setOnClickListener { loadLogs() }

        loadLogs()
    }

    private fun loadLogs() {
        logDir?.let { dir ->
            if (!dir.exists()) {
                logText.text = "No logs directory found."
                return
            }

            val logFiles = dir.listFiles()
                ?.filter { it.extension == "log" || it.name.contains("log", ignoreCase = true) }
                ?.sortedByDescending { it.lastModified() }
                .orEmpty()

            if (logFiles.isEmpty()) {
                logText.text = "No log files found."
                return
            }

            // Read the most recent log file
            val latestLog = logFiles[0]
            val content = latestLog.readText()
            logText.text = content

            // Auto-scroll to bottom
            logText.post {
                logText.scrollTo(0, logText.layout?.lineCount ?: 0)
            }
        }
    }

    companion object {
        fun newInstance(instanceName: String, logDir: String): LogsFragment {
            val fragment = LogsFragment()
            val args = Bundle()
            args.putString("instance_name", instanceName)
            args.putString("log_dir", logDir)
            fragment.arguments = args
            return fragment
        }
    }
}
