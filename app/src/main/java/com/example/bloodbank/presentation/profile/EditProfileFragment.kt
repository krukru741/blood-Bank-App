package com.example.bloodbank.presentation.profile

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.bloodbank.R
import com.example.bloodbank.databinding.FragmentEditProfileBinding
import com.example.bloodbank.domain.model.User
import com.example.bloodbank.domain.model.UserRole
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditProfileViewModel by viewModels()

    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            Glide.with(this)
                .load(it)
                .into(binding.ivEditAvatar)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        binding.toolbarEditProfile.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.fabChangePhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnSaveProfile.setOnClickListener {
            viewModel.updateProfile(
                displayName = binding.etFullName.text.toString(),
                phoneNumber = binding.etPhone.text.toString(),
                city = binding.etCity.text.toString(),
                weightKgText = binding.etWeight.text.toString(),
                photoUri = selectedImageUri
            )
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currentUser.collect { user ->
                        user?.let { populateFields(it) }
                    }
                }

                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is EditProfileUiState.Idle -> {
                                binding.progressOverlay.isVisible = false
                            }
                            is EditProfileUiState.Loading -> {
                                binding.progressOverlay.isVisible = true
                            }
                            is EditProfileUiState.Success -> {
                                binding.progressOverlay.isVisible = false
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                                viewModel.resetState()
                                findNavController().navigateUp()
                            }
                            is EditProfileUiState.Error -> {
                                binding.progressOverlay.isVisible = false
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                                viewModel.resetState()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun populateFields(user: User) {
        if (binding.etFullName.text.isNullOrBlank()) {
            binding.etFullName.setText(user.displayName)
            binding.etPhone.setText(user.phoneNumber)
            binding.etCity.setText(user.city)

            if (user.role == UserRole.DONOR) {
                binding.tilWeight.isVisible = true
                binding.etWeight.setText(user.weightKg?.toString() ?: "")
            } else {
                binding.tilWeight.isVisible = false
            }

            if (user.profilePhotoUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(user.profilePhotoUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(binding.ivEditAvatar)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
