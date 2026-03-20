package com.apollo.socially.ui.main

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.apollo.socially.R
import com.apollo.socially.utils.MediaPermissionHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainContainerActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var fabCreate: FloatingActionButton
    private lateinit var bottomNavContainer: View
    private lateinit var navController: NavController

    private val hiddenNavDestinations = setOf(
        R.id.settingsFragment,
        R.id.postDetailFragment,
        R.id.notificationsFragment,
        R.id.storyViewerFragment,
        R.id.createPostPickerFragment,
        R.id.createPostCaptionFragment,
        R.id.profilePostFeedFragment,
        R.id.editProfileFragment
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_main) as? NavHostFragment
        val currentFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull()
        MediaPermissionHelper.handleResult(
            currentFragment ?: return@registerForActivityResult, results
        ) { navController.navigate(R.id.createPostPickerFragment) }
    }

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
        setupBackPress()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
        navController = navHostFragment.navController

        bottomNav.setupWithNavController(navController)
        fabCreate.setOnClickListener { openCreatePicker() }
        bottomNav.menu.findItem(R.id.navigation_create)?.isEnabled = false

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id in hiddenNavDestinations) hideBottomNav() else showBottomNav()
        }
    }

    private fun openCreatePicker() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_main) as? NavHostFragment
        val currentFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull() ?: return
        MediaPermissionHelper.checkAndRequest(currentFragment, permissionLauncher) {
            navController.navigate(R.id.createPostPickerFragment)
        }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!navController.popBackStack()) finish()
            }
        })
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
}
