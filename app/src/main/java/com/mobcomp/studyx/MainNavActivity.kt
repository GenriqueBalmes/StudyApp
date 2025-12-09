package com.mobcomp.studyx

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainNavActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get layout ID dynamically
        val layoutId = resources.getIdentifier("activity_main_nav", "layout", packageName)
        setContentView(layoutId)

        // Get bottom navigation view
        val bottomNavId = resources.getIdentifier("bottom_navigation", "id", packageName)
        val bottomNav = findViewById<BottomNavigationView>(bottomNavId)

        // Load Home fragment by default
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        // Handle bottom navigation item clicks
        bottomNav.setOnItemSelectedListener { item ->
            val navHomeId = resources.getIdentifier("nav_home", "id", packageName)
            val navTasksId = resources.getIdentifier("nav_tasks", "id", packageName)
            val navTimerId = resources.getIdentifier("nav_timer", "id", packageName)
            val navFlashcardsId = resources.getIdentifier("nav_flashcards", "id", packageName)

            when (item.itemId) {
                navHomeId -> {
                    loadFragment(HomeFragment())
                    true
                }
                navTasksId -> {
                    loadFragment(TasksFragment())
                    true
                }
                navTimerId -> {
                    loadFragment(TimerFragment())
                    true
                }
                navFlashcardsId -> {
                    loadFragment(FlashcardsFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        // Get fragment container ID
        val fragmentContainerId = resources.getIdentifier("fragment_container", "id", packageName)

        supportFragmentManager.beginTransaction()
            .replace(fragmentContainerId, fragment)
            .commit()
    }
}