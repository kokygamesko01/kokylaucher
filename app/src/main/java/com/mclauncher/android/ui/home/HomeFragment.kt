package com.mclauncher.android.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.mclauncher.android.R

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnCreateInstance = view.findViewById<Button>(R.id.btn_create_instance)
        val btnVersions = view.findViewById<Button>(R.id.btn_versions)
        val tvWelcome = view.findViewById<TextView>(R.id.tv_welcome)

        tvWelcome.text = "Welcome to MC Launcher"

        btnCreateInstance.setOnClickListener {
            // Navigate to instances screen
            requireActivity().onBackPressed() // placeholder - handled by nav
        }

        btnVersions.setOnClickListener {
            // Navigate to versions screen
        }
    }
}
