package com.mclauncher.android

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mclauncher.android.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Apply fade-in to the whole splash layout
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        binding.root.startAnimation(fadeIn)

        // Apply pulse animation to the logo
        val pulse = AnimationUtils.loadAnimation(this, R.anim.logo_pulse)
        binding.splashLogo.startAnimation(pulse)

        // Show loading message after a brief moment
        binding.splashLoadingMessage.visibility = View.VISIBLE

        // Transition to login after splash completes
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing) {
                fadeOutAndGoToLogin()
            }
        }, 3000)
    }

    private fun fadeOutAndGoToLogin() {
        val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out)
        fadeOut.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}

            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                if (!isFinishing) {
                    val intent = Intent(this@SplashActivity, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }

            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })
        binding.root.startAnimation(fadeOut)
    }
}
