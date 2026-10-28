package com.mclauncher.android.ui.settings

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.mclauncher.android.R

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnAbout = view.findViewById<Button>(R.id.btn_about)
        btnAbout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("About MC Launcher")
                .setMessage(requireContext().getString(R.string.about_text))
                .setPositiveButton("OK", null)
                .show()
        }
    }
}
