package com.example.bloodbank.presentation.auth.register

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
import com.example.bloodbank.databinding.FragmentRegisterBinding
import com.example.bloodbank.presentation.common.AuthUiState
import com.example.bloodbank.presentation.common.extensions.clearError
import com.example.bloodbank.presentation.common.extensions.hide
import com.example.bloodbank.presentation.common.extensions.hideKeyboard
import com.example.bloodbank.presentation.common.extensions.show
import com.example.bloodbank.presentation.common.extensions.showErrorSnackbar
import com.example.bloodbank.presentation.common.extensions.showSuccessSnackbar
import com.example.bloodbank.presentation.common.extensions.text
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTextChangeListeners()
        setupButtonListeners()
        observeUiState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupTextChangeListeners() {
        with(binding) {
            etFullName.addTextChangedListener        { tilFullName.clearError() }
            etEmail.addTextChangedListener           { tilEmail.clearError() }
            etPhone.addTextChangedListener           { tilPhone.clearError() }
            etPassword.addTextChangedListener        { tilPassword.clearError() }
            etConfirmPassword.addTextChangedListener { tilConfirmPassword.clearError() }
        }
    }

    private fun setupButtonListeners() {
        with(binding) {
            btnRegister.setOnClickListener {
                hideKeyboard()
                if (!cbTerms.isChecked) {
                    binding.coordinatorRegister.showErrorSnackbar("You must agree to the Terms & Conditions.")
                    return@setOnClickListener
                }

                viewModel.register(
                    fullName          = tilFullName.text,
                    email             = tilEmail.text,
                    phone             = tilPhone.text,
                    password          = tilPassword.text,
                    confirmPassword   = tilConfirmPassword.text
                )
            }

            tvLoginLink.setOnClickListener {
                findNavController().navigate(R.id.action_register_to_login)
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
                        is AuthUiState.Success -> onRegisterSuccess()
                        is AuthUiState.Error   -> onError(state.message)
                    }
                }
            }
        }
    }

    private fun onLoading() {
        setFormEnabled(false)
        binding.progressRegister.show()
    }

    private fun onRegisterSuccess() {
        binding.progressRegister.hide()
        findNavController().navigate(R.id.action_register_to_login)
        requireActivity().window.decorView.showSuccessSnackbar(
            "✅ Account created! Please verify your email before logging in."
        )
    }

    private fun onError(message: String) {
        binding.progressRegister.hide()
        setFormEnabled(true)
        binding.coordinatorRegister.showErrorSnackbar(message)
        viewModel.resetState()
    }

    private fun setFormEnabled(enabled: Boolean) {
        with(binding) {
            etFullName.isEnabled          = enabled
            etEmail.isEnabled             = enabled
            etPhone.isEnabled             = enabled
            etPassword.isEnabled          = enabled
            etConfirmPassword.isEnabled   = enabled
            btnRegister.isEnabled         = enabled
            cbTerms.isEnabled             = enabled
        }
    }
}
