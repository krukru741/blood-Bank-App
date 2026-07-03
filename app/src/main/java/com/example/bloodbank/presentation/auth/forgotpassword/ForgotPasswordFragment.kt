package com.example.bloodbank.presentation.auth.forgotpassword

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
import com.example.bloodbank.databinding.FragmentForgotPasswordBinding
import com.example.bloodbank.presentation.common.AuthUiState
import com.example.bloodbank.presentation.common.extensions.clearError
import com.example.bloodbank.presentation.common.extensions.hide
import com.example.bloodbank.presentation.common.extensions.hideKeyboard
import com.example.bloodbank.presentation.common.extensions.setError
import com.example.bloodbank.presentation.common.extensions.show
import com.example.bloodbank.presentation.common.extensions.showErrorSnackbar
import com.example.bloodbank.presentation.common.extensions.showSuccessSnackbar
import com.example.bloodbank.presentation.common.extensions.text
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * ForgotPasswordFragment
 *
 * Simple screen: enter email → request password reset link.
 * On success: navigate back to Login and show a success Snackbar.
 */
@AndroidEntryPoint
class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ForgotPasswordViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeUiState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupListeners() {
        with(binding) {
            etEmail.addTextChangedListener { tilEmail.clearError() }

            btnSendReset.setOnClickListener {
                hideKeyboard()
                viewModel.sendResetEmail(tilEmail.text)
            }

            btnBackToLogin.setOnClickListener {
                findNavController().navigateUp()
            }
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is AuthUiState.Idle    -> setFormEnabled(true)
                        is AuthUiState.Loading -> onLoading()
                        is AuthUiState.Success -> onSuccess()
                        is AuthUiState.Error   -> onError(state.message)
                    }
                }
            }
        }
    }

    private fun onLoading() {
        setFormEnabled(false)
        binding.progressForgot.show()
    }

    private fun onSuccess() {
        binding.progressForgot.hide()
        // Navigate back and show confirmation
        findNavController().navigateUp()
        requireActivity().window.decorView.showSuccessSnackbar(
            "Password reset email sent! Check your inbox."
        )
    }

    private fun onError(message: String) {
        binding.progressForgot.hide()
        setFormEnabled(true)
        // If it looks like a field validation error, show it inline
        if (message.contains("email", ignoreCase = true) && !message.contains("failed", ignoreCase = true)) {
            binding.tilEmail.setError(message)
        } else {
            binding.coordinatorForgot.showErrorSnackbar(message)
        }
        viewModel.resetState()
    }

    private fun setFormEnabled(enabled: Boolean) {
        with(binding) {
            etEmail.isEnabled      = enabled
            btnSendReset.isEnabled = enabled
        }
    }
}
