package com.mclauncher.android

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mclauncher.android.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Top bar
        binding.btnHome.setOnClickListener {
            // Home icon - go back or refresh
        }

        // Login button
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            if (TextUtils.isEmpty(username)) {
                Toast.makeText(this, "Zadejte uživatelské jméno", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save username locally and go to main
            val prefs = getSharedPreferences("kokylaucher_prefs", MODE_PRIVATE)
            prefs.edit().putString("player_name", username).apply()

            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("player_name", username)
            startActivity(intent)
            finish()
        }
    }
}
