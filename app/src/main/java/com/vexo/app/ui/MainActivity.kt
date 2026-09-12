package com.vexo.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.vexo.app.R
import com.vexo.app.databinding.ActivityMainBinding
import com.vexo.app.ui.fragments.AiLabFragment
import com.vexo.app.ui.fragments.DiscoverFragment
import com.vexo.app.ui.fragments.HomeFragment
import com.vexo.app.ui.fragments.ProfileFragment
import com.vexo.app.ui.fragments.ProjectsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadFragment(HomeFragment())

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_edit -> loadFragment(HomeFragment())
                R.id.nav_discover -> loadFragment(DiscoverFragment())
                R.id.nav_ailab -> loadFragment(AiLabFragment())
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
