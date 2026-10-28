package com.mclauncher.android

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Theme provides windowBackground (@drawable/splash_background) for the splash image

        // Show splash for 2 seconds, then go to login
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing) {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        }, 2000)
    }
}
