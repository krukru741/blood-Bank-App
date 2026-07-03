package com.example.bloodbank.presentation.auth.login

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.bloodbank.R
import com.example.bloodbank.databinding.FragmentLoginBinding
import com.example.bloodbank.presentation.common.AuthUiState
import com.example.bloodbank.presentation.common.extensions.clearError
import com.example.bloodbank.presentation.common.extensions.hide
import com.example.bloodbank.presentation.common.extensions.hideKeyboard
import com.example.bloodbank.presentation.common.extensions.setError
import com.example.bloodbank.presentation.common.extensions.show
import com.example.bloodbank.presentation.common.extensions.showErrorSnackbar
import com.example.bloodbank.presentation.common.extensions.text
import com.example.bloodbank.presentation.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * LoginFragment
 *
 * Handles the login UI:
 * - Collects user input
 * - Clears field errors as the user types
 * - Delegates to [LoginViewModel] for business logic
 * - Observes [AuthUiState] and reacts accordingly:
 *   Loading → show progress
 *   Success → navigate to MainActivity
 *   Error   → show Snackbar + re-enable form
 *
 * @AndroidEntryPoint — required for Hilt injection in Fragments.
 */
@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeUiState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null   // Prevent memory leaks
    }

    // ── Social Sign-In ─────────────────────────────────────────────────────────

    private val googleSignInLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                account.idToken?.let { idToken ->
                    viewModel.signInWithGoogle(idToken)
                } ?: run {
                    onError("Google sign in failed: ID Token is null")
                }
            } catch (e: com.google.android.gms.common.api.ApiException) {
                onError("Google sign in failed: ${e.message}")
            }
        }
    }

    private val callbackManager = com.facebook.CallbackManager.Factory.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.facebook.login.LoginManager.getInstance().registerCallback(callbackManager, object : com.facebook.FacebookCallback<com.facebook.login.LoginResult> {
            override fun onSuccess(result: com.facebook.login.LoginResult) {
                viewModel.signInWithFacebook(result.accessToken.token)
            }
            override fun onCancel() {
                onError("Facebook login cancelled.")
            }
            override fun onError(error: com.facebook.FacebookException) {
                onError("Facebook login failed: ${error.message}")
            }
        })
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    // ── UI Setup ───────────────────────────────────────────────────────────────

    private fun setupListeners() {
        with(binding) {
            // Clear inline errors as user types
            etEmail.addTextChangedListener    { tilEmail.clearError() }
            etPassword.addTextChangedListener { tilPassword.clearError() }

            // Login button → delegate to ViewModel
            btnLogin.setOnClickListener {
                hideKeyboard()
                viewModel.login(
                    email      = tilEmail.text,
                    password   = tilPassword.text,
                    rememberMe = cbRememberMe.isChecked
                )
            }

            // Navigate to Register
            btnCreateAccount.setOnClickListener {
                findNavController().navigate(R.id.action_login_to_register)
            }

            // Navigate to Forgot Password
            btnForgotPassword.setOnClickListener {
                findNavController().navigate(R.id.action_login_to_forgot_password)
            }
            
            // Social Auth Buttons
            btnGoogle.setOnClickListener {
                val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(requireActivity(), gso)
                googleSignInClient.signOut() // Clear any cached session first
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
            
            btnFacebook.setOnClickListener {
                com.facebook.login.LoginManager.getInstance().logInWithReadPermissions(
                    this@LoginFragment, listOf("email", "public_profile")
                )
            }
        }
    }

    // ── State Observation ──────────────────────────────────────────────────────

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            // repeatOnLifecycle ensures we stop collecting when Fragment is paused
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is AuthUiState.Idle    -> setFormEnabled(true)
                            is AuthUiState.Loading -> onLoading()
                            is AuthUiState.Success -> onLoginSuccess()
                            is AuthUiState.Error   -> onError(state.message)
                        }
                    }
                }
                
                launch {
                    viewModel.savedEmail.collect { email ->
                        if (!email.isNullOrEmpty()) {
                            binding.etEmail.setText(email)
                            binding.cbRememberMe.isChecked = true
                        }
                    }
                }
            }
        }
    }

    // ── State Handlers ─────────────────────────────────────────────────────────

    private fun onLoading() {
        setFormEnabled(false)
        binding.progressLogin.show()
    }

    private fun onLoginSuccess() {
        binding.progressLogin.hide()
        // Navigate to MainActivity and clear the back stack
        // (User should not be able to press back to return to Login)
        val intent = Intent(requireActivity(), MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    private fun onError(message: String) {
        binding.progressLogin.hide()
        setFormEnabled(true)
        binding.coordinatorLogin.showErrorSnackbar(message)
        viewModel.resetState()
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private fun setFormEnabled(enabled: Boolean) {
        with(binding) {
            etEmail.isEnabled           = enabled
            etPassword.isEnabled        = enabled
            btnLogin.isEnabled          = enabled
            btnCreateAccount.isEnabled  = enabled
            btnForgotPassword.isEnabled = enabled
        }
    }
}
