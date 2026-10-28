package com.mclauncher.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mclauncher.android.R
import com.mclauncher.android.ui.launch.LaunchActivity

class LaunchService : Service() {

    private lateinit var notificationManager: NotificationManager
    private var notificationId = 1001

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Forward to LaunchActivity
        val launchIntent = Intent(this, LaunchActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, "koky_launch_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Kokylaucher")
            .setContentText("Minecraft launch in progress")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(notificationId, notification)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "koky_launch_channel",
                "Kokylaucher Launch",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "Shows Minecraft launch progress"
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun stopForegroundService() {
        stopForeground(true)
        stopSelf()
    }
}
