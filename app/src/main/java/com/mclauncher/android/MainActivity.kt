package com.mclauncher.android

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mclauncher.android.ui.home.HomeFragment
import com.mclauncher.android.ui.instances.InstancesFragment
import com.mclauncher.android.ui.mods.ModsFragment
import com.mclauncher.android.ui.settings.SettingsFragment
import com.mclauncher.android.ui.versions.VersionsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottom_navigation)

        // Default to Home
        if (savedInstanceState == null) {
            navigateTo(R.id.nav_home)
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            navigateTo(item.itemId)
            true
        }
    }

    private fun navigateTo(destinationId: Int) {
        val fragment: Fragment = when (destinationId) {
            R.id.nav_home -> HomeFragment()
            R.id.nav_versions -> VersionsFragment()
            R.id.nav_instances -> InstancesFragment()
            R.id.nav_mods -> ModsFragment()
            R.id.nav_settings -> SettingsFragment()
            else -> HomeFragment()
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
