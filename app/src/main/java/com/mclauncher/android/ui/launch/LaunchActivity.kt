package com.mclauncher.android.ui.launch

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mclauncher.android.R
import com.mclauncher.android.service.LaunchManager
import com.mclauncher.android.service.LaunchResult

class LaunchActivity : AppCompatActivity() {

    private lateinit var launchManager: LaunchManager
    private lateinit var logText: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var btnClose: Button

    private val logUpdateHandler by lazy { Handler(Looper.getMainLooper()) }
    private val logUpdateRunnable = object : Runnable {
        override fun run() {
            logText.text = launchManager.getLaunchLog()
            scrollView.fullScroll(ScrollView.FOCUS_DOWN)
            logUpdateHandler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)

        launchManager = LaunchManager(this)

        logText = findViewById(R.id.launchLogText)
        scrollView = findViewById(R.id.launchLogScroll)
        btnClose = findViewById(R.id.btnLaunchClose)

        btnClose.setOnClickListener {
            stopLaunchService()
            finish()
        }

        // Start listening for launch log updates
        startLogUpdates()

        // Perform the launch
        val instanceId = intent.getStringExtra("instance_id")
        val versionId = intent.getStringExtra("version_id")

        if (instanceId != null) {
            val instance = launchManagerInstance(instanceId)
            if (instance != null) {
                val version = if (versionId != null) {
                    com.mclauncher.android.data.model.Version(
                        id = versionId,
                        type = "release",
                        url = "",
                        time = "",
                        releaseTime = ""
                    )
                } else {
                    null
                }
                val result = launchManager.launch(instance, version)
                showLaunchResult(result)
            } else {
                logText.append("\nERROR: Instance not found: $instanceId\n")
            }
        } else {
            logText.append("\nERROR: No instance specified\n")
        }
    }

    private fun launchManagerInstance(instanceId: String): com.mclauncher.android.data.model.Instance? {
        val instances = com.mclauncher.android.data.repository.InstanceRepository(this).listInstances()
        return instances.find { it.id == instanceId }
    }

    private fun startLogUpdates() {
        logUpdateHandler.post(logUpdateRunnable)
    }

    private fun stopLogUpdates() {
        logUpdateHandler.removeCallbacks(logUpdateRunnable)
    }

    private fun showLaunchResult(result: LaunchResult) {
        when (result) {
            is LaunchResult.Success -> {
                logText.append("\n=== Minecraft process started successfully ===\n")
                logText.append("The game is running. Check the logs above for output.\n")
            }
            is LaunchResult.Failure -> {
                logText.append("\n=== LAUNCH FAILED ===\n")
                logText.append("Error: ${result.exception.message}\n")
                logText.append("See full log above for details.\n")
            }
            is LaunchResult.MissingRuntime -> {
                logText.append("\n=== MISSING JAVA RUNTIME ===\n")
                logText.append("${result.message}\n")
            }
            is LaunchResult.MissingNatives -> {
                logText.append("\n=== MISSING LWJGL NATIVES ===\n")
                logText.append("${result.message}\n")
            }
        }
    }

    private fun stopLaunchService() {
        val intent = android.content.Intent(this, com.mclauncher.android.service.LaunchService::class.java)
        stopService(intent)
    }

    override fun onDestroy() {
        stopLogUpdates()
        super.onDestroy()
    }
}
