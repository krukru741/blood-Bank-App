package com.example.bloodbank.presentation.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.bloodbank.presentation.auth.AuthActivity
import com.example.bloodbank.presentation.main.MainActivity
import com.example.bloodbank.R
import dagger.hilt.android.AndroidEntryPoint

/**
 * SplashActivity
 *
 * Entry point of the app. Uses the AndroidX Splash Screen API (Android 12 compat).
 * Decides whether to route the user to [AuthActivity] or [MainActivity]
 * based on their login state.
 *
 * @AndroidEntryPoint — required for Hilt injection in Activities.
 */
@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called BEFORE super.onCreate()
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Show our custom layout
        setContentView(R.layout.activity_splash)

        // Do not keep the system splash screen on screen indefinitely.
        // It will quickly transition to our activity_splash.xml
        // which contains the uncropped logo and text.

        // Observe navigation destination (set by ViewModel after checking auth state)
        viewModel.navigationDestination.observe(this) { destination ->
            val intent = when (destination) {
                SplashViewModel.Destination.MAIN -> Intent(this, MainActivity::class.java)
                SplashViewModel.Destination.AUTH -> Intent(this, AuthActivity::class.java)
            }
            startActivity(intent)
            finish() // Remove SplashActivity from back stack
        }
    }
}
