package com.mclauncher.android.ui.mods

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.mclauncher.android.R

class ModsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mods, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnInstall = view.findViewById<Button>(R.id.btn_install_mod)
        btnInstall.setOnClickListener {
            Toast.makeText(requireContext(), "Mod installation coming soon", Toast.LENGTH_SHORT).show()
        }
    }
}
