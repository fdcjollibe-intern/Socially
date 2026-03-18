package com.apollo.socially.ui.main

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.apollo.socially.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainContainerActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var fabCreate: FloatingActionButton
    private lateinit var bottomNavContainer: View

    private val hiddenNavDestinations = setOf(
        R.id.settingsFragment,
        R.id.postDetailFragment,
        R.id.notificationsFragment,
        R.id.storyViewerFragment   // ← story viewer is full screen, no nav bar
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_with_nav)
        supportActionBar?.hide()

        bottomNav = findViewById(R.id.bottom_navigation)
        fabCreate = findViewById(R.id.fab_create)
        bottomNavContainer = findViewById(R.id.bottomNavigationContainer)

        fabCreate.bringToFront()
        fabCreate.invalidate()

        setupNavigation()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
        val navController = navHostFragment.navController

        bottomNav.setupWithNavController(navController)
        fabCreate.setOnClickListener { navController.navigate(R.id.navigation_create) }
        bottomNav.menu.findItem(R.id.navigation_create)?.isEnabled = false

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id in hiddenNavDestinations) hideBottomNav() else showBottomNav()
        }
    }

    private fun showBottomNav() {
        if (bottomNavContainer.visibility == View.VISIBLE) return
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.nav_slide_up)
        bottomNavContainer.visibility = View.VISIBLE
        bottomNavContainer.startAnimation(slideUp)
        fabCreate.visibility = View.VISIBLE
        fabCreate.bringToFront()
        fabCreate.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(300).setStartDelay(100).start()
    }

    private fun hideBottomNav() {
        if (bottomNavContainer.visibility == View.GONE) return
        val slideDown = AnimationUtils.loadAnimation(this, R.anim.nav_slide_down)
        slideDown.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(a: android.view.animation.Animation?) {}
            override fun onAnimationRepeat(a: android.view.animation.Animation?) {}
            override fun onAnimationEnd(a: android.view.animation.Animation?) {
                bottomNavContainer.visibility = View.GONE
            }
        })
        bottomNavContainer.startAnimation(slideDown)
        fabCreate.animate().alpha(0f).scaleX(0.8f).scaleY(0.8f).setDuration(250)
            .withEndAction { fabCreate.visibility = View.GONE }.start()
    }

    override fun onBackPressed() {
        finish()
    }
}