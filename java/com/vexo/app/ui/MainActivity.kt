package com.vexo.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.vexo.app.R
import com.vexo.app.ui.fragments.AiLabFragment
import com.vexo.app.ui.fragments.DiscoverFragment
import com.vexo.app.ui.fragments.HomeFragment
import com.vexo.app.ui.fragments.ProfileFragment
import com.vexo.app.ui.fragments.ProjectsFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // Default fragment
        loadFragment(HomeFragment())

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_edit -> loadFragment(HomeFragment())
                R.id.nav_discover -> loadFragment(DiscoverFragment())
                R.id.nav_ai -> loadFragment(AiLabFragment())
                R.id.nav_projects -> loadFragment(ProjectsFragment())
                R.id.nav_me -> loadFragment(ProfileFragment())
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
