package com.mclauncher.android.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mclauncher.android.R
import com.mclauncher.android.databinding.ActivityLanguageSettingsBinding
import com.mclauncher.android.util.LocaleHelper
import com.mclauncher.android.util.LocaleHelper.getSelectedLanguage
import com.mclauncher.android.util.LocaleHelper.saveSelectedLanguage
import com.mclauncher.android.util.SupportedLanguages

class LanguageSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLanguageSettingsBinding
    private var selectedLanguageCode: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back button
        binding.btnBack.setOnClickListener { finish() }

        selectedLanguageCode = getSelectedLanguage(this).code

        val layoutLanguages = findViewById<android.widget.LinearLayout>(R.id.layoutLanguages)

        for (lang in SupportedLanguages.all) {
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_language, layoutLanguages, false)
            layoutLanguages.addView(itemView)

            val tvName: TextView = itemView.findViewById(R.id.tvLanguageName)
            val rbSelect: RadioButton = itemView.findViewById(R.id.rbLanguageSelect)

            tvName.text = lang.name
            rbSelect.isChecked = lang.code == selectedLanguageCode

            rbSelect.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedLanguageCode = lang.code
                    // Unselect others
                    for (i in 0 until layoutLanguages.childCount) {
                        val child = layoutLanguages.getChildAt(i)
                        val otherRb = child.findViewById<android.widget.RadioButton>(R.id.rbLanguageSelect)
                        if (otherRb != null && otherRb != rbSelect) {
                            otherRb.isChecked = false
                        }
                    }
                    // Apply language change
                    saveSelectedLanguage(this, lang)
                    Toast.makeText(this, "Language changed to ${lang.name}. Restarting app...", Toast.LENGTH_SHORT).show()
                    // Recreate to apply new language immediately
                    recreate()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // If language was changed in a previous activity, recreate to apply
        val currentCode = com.mclauncher.android.util.LocaleHelper.getSelectedLanguage(this).code
        if (currentCode != selectedLanguageCode) {
            recreate()
        }
    }
}
