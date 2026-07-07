package com.example.bloodbank.presentation.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import kotlinx.coroutines.launch
import com.example.bloodbank.R
import com.example.bloodbank.databinding.ActivityMainBinding
import com.example.bloodbank.domain.repository.AuthRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * MainActivity — single-Activity shell for the main app (post-login).
 *
 * Architecture:
 * - activity_main.xml hosts [NavHostFragment] (main_graph.xml) +
 *   [BottomNavigationView] + [MaterialToolbar]
 * - [NavigationUI] automatically syncs BottomNav + Toolbar with [NavController]
 * - Back-stack is managed by the NavController
 * - Top-level destinations:  Home, MyRequests, Profile, Activity
 * - Full-screen destinations: CreateRequest, RequestDetail
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var userRepository: com.example.bloodbank.domain.repository.UserRepository

    // Permission launcher for Android 13+ Notifications
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM can post notifications
        } else {
            // Inform user that notifications won't be shown
        }
    }

    /**
     * Top-level destinations — BottomNav tabs that show NO back arrow.
     */
    private val topLevelDestinations = setOf(
        R.id.homeFragment,
        R.id.myRequestsFragment,
        R.id.activityFragment,
        R.id.profileFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        askNotificationPermission()
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupNavigation() {
        binding.bottomNav.inflateMenu(R.menu.bottom_nav_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.main_nav_host) as NavHostFragment
        navController = navHostFragment.navController



        binding.bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.action_more) {
                com.example.bloodbank.presentation.more.MoreMenuBottomSheet()
                    .show(supportFragmentManager, "MoreMenu")
                return@setOnItemSelectedListener false
            }
            NavigationUI.onNavDestinationSelected(item, navController)
        }

        navController.addOnDestinationChangedListener(destinationChangedListener)

        // Setup Top App Bar Avatar click listener
        binding.ivToolbarAvatar.setOnClickListener {
            com.example.bloodbank.presentation.more.MoreMenuBottomSheet()
                .show(supportFragmentManager, "MoreMenu")
        }

        // Setup Top App Bar Filter click listener
        binding.btnToolbarFilter.setOnClickListener {
            com.example.bloodbank.presentation.home.FilterBottomSheetFragment()
                .show(supportFragmentManager, "FilterBottomSheet")
        }

        // Load Avatar image
        authRepository.currentUser?.uid?.let { uid ->
            lifecycleScope.launch {
                userRepository.observeCurrentUser(uid).collect { resource ->
                    if (resource is com.example.bloodbank.domain.model.Resource.Success) {
                        val photoUrl = resource.data.profilePhotoUrl
                        if (!photoUrl.isNullOrEmpty()) {
                            com.bumptech.glide.Glide.with(this@MainActivity)
                                .load(photoUrl)
                                .placeholder(android.R.drawable.ic_menu_my_calendar)
                                .into(binding.ivToolbarAvatar)
                        }
                    }
                }
            }
        }
    }

    private val destinationChangedListener = NavController.OnDestinationChangedListener { _, destination, _ ->
        if (destination.id != R.id.action_more) {
            binding.bottomNav.menu.findItem(destination.id)?.isChecked = true
        }
        
        val isTopLevel = destination.id in topLevelDestinations
        
        
        // Show the entire App Bar (Logo, Filter, Avatar) ONLY on HomeFragment
        val isHome = destination.id == R.id.homeFragment
        binding.appBarLayout.visibility = if (isHome) android.view.View.VISIBLE else android.view.View.GONE

        // Hide bottom navigation on full-screen destinations
        binding.bottomNav.visibility = if (isTopLevel) android.view.View.VISIBLE else android.view.View.GONE
    }
}
