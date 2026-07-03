package com.example.bloodbank.presentation.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.bloodbank.databinding.ActivityAuthBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * AuthActivity
 *
 * Single-Activity host for all authentication screens.
 * The NavHostFragment inside [activity_auth.xml] drives:
 *   LoginFragment → RegisterFragment / ForgotPasswordFragment
 *
 * Uses ViewBinding — no more findViewById().
 * @AndroidEntryPoint allows Hilt to inject into this Activity if needed.
 */
@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Navigation is fully handled by the NavHostFragment in the layout.
        // No additional setup required here.
    }
}
