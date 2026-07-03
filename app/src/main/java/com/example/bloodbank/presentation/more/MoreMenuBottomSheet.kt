package com.example.bloodbank.presentation.more

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.bloodbank.R
import com.example.bloodbank.databinding.FragmentMoreBottomSheetBinding
import com.example.bloodbank.domain.model.Resource
import com.example.bloodbank.domain.repository.AuthRepository
import com.example.bloodbank.domain.repository.UserRepository
import com.example.bloodbank.presentation.auth.AuthActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MoreMenuBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentMoreBottomSheetBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var userRepository: UserRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoreBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val uid = authRepository.currentUser?.uid ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            userRepository.observeCurrentUser(uid).collect { resource ->
                if (resource is Resource.Success) {
                    val user = resource.data
                    binding.tvMoreName.text = user.displayName
                    binding.tvMoreEmail.text = user.email

                    if (!user.profilePhotoUrl.isNullOrEmpty()) {
                        Glide.with(this@MoreMenuBottomSheet)
                            .load(user.profilePhotoUrl)
                            .placeholder(android.R.drawable.ic_menu_my_calendar)
                            .into(binding.ivMoreAvatar)
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        // 1. Hospital Partners / Find a Hospital
        binding.rowHospitals.setOnClickListener {
            val navHostFragment = requireActivity().supportFragmentManager
                .findFragmentById(R.id.main_nav_host) as androidx.navigation.fragment.NavHostFragment
            val bundle = Bundle().apply { putString("initialFilter", "HOSPITALS") }
            navHostFragment.navController.navigate(R.id.homeFragment, bundle)
            dismiss()
        }

        // 2. My Certificates & Badges
        binding.rowBadges.setOnClickListener {
            // TODO: Navigate to Gamification / Badges
            showToast("Badges & Certificates coming soon!")
            dismiss()
        }

        // 3. Emergency Hotline
        binding.rowEmergency.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:911") // Replace with actual BloodBank emergency hotline
            }
            startActivity(intent)
            dismiss()
        }

        // 4. Donation Eligibility Quiz / Checklist
        binding.rowQuiz.setOnClickListener {
            val navHostFragment = requireActivity().supportFragmentManager
                .findFragmentById(R.id.main_nav_host) as androidx.navigation.fragment.NavHostFragment
            navHostFragment.navController.navigate(R.id.eligibilityFragment)
            dismiss()
        }

        // 5. Settings
        binding.rowSettings.setOnClickListener {
            val navHostFragment = requireActivity().supportFragmentManager
                .findFragmentById(R.id.main_nav_host) as androidx.navigation.fragment.NavHostFragment
            navHostFragment.navController.navigate(R.id.settingsFragment)
            dismiss()
        }

        // 6. Privacy Policy
        binding.rowPrivacy.setOnClickListener {
            showToast("Privacy Policy coming soon!")
            dismiss()
        }

        // 7. About Us
        binding.rowAbout.setOnClickListener {
            showToast("About BloodBank v1.0")
            dismiss()
        }

        // 8. Share App
        binding.rowShare.setOnClickListener {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "Save a life today! Download the BloodBank app: https://bloodbank.example.com")
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
            dismiss()
        }

        // 9. Log Out
        binding.rowLogout.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                authRepository.logout()
                navigateToAuth()
            }
        }
    }

    private fun navigateToAuth() {
        val intent = Intent(requireActivity(), AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
