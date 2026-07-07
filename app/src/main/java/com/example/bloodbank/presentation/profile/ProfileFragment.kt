package com.example.bloodbank.presentation.profile

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.bloodbank.R
import com.example.bloodbank.databinding.FragmentProfileBinding
import com.example.bloodbank.domain.model.User
import com.example.bloodbank.domain.model.UserRole
import com.example.bloodbank.presentation.common.extensions.showErrorSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ProfileFragment
 *
 * Shows the current user's profile with:
 * - Avatar, name, email, blood type badge, role badge
 * - Stats card (role-dependent):
 *   Donors: total donations (placeholder), next eligible date, eligibility
 *   Recipients: active requests (placeholder)
 * - Info section: phone, city, weight (donors), last donation (donors)
 * - Edit Profile and Sign Out actions
 */
@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
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

    // ── Listeners ──────────────────────────────────────────────────────────────

    private fun setupListeners() {
        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_edit_profile)
        }
        binding.btnCompleteProfile.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_become_donor)
        }
        binding.switchAvailable.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                viewModel.toggleAvailability(isChecked)
            }
        }
        binding.btnViewDonorCard.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_donor_card)
        }
    }

    // ── State Observation ──────────────────────────────────────────────────────

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is ProfileUiState.Loading   -> { /* no-op: data is loading */ }
                        is ProfileUiState.Success   -> bindUser(state.user)
                        is ProfileUiState.Error     ->
                            binding.coordinatorProfile.showErrorSnackbar(state.message)
                        is ProfileUiState.SignedOut  -> { /* Handled by More Menu now */ }
                    }
                }
            }
        }
    }

    // ── Bind User Data ─────────────────────────────────────────────────────────

    private fun bindUser(user: User) {
        with(binding) {
            // Header
            tvProfileName.text        = user.displayName.ifBlank { "Unknown User" }
            tvProfileEmail.text       = user.email
            tvBloodTypeBadge.text     = user.bloodType.label
            tvRoleBadge.text = when (user.role) {
                UserRole.DONOR -> "🩸 Donor"
                UserRole.RECIPIENT -> "🏥 Recipient"
                UserRole.USER -> "👤 User"
                else -> "⚙️ Admin"
            }

            if (user.profilePhotoUrl.isNotEmpty()) {
                Glide.with(this@ProfileFragment)
                    .load(user.profilePhotoUrl)
                    .placeholder(android.R.drawable.ic_menu_my_calendar)
                    .into(ivAvatar)
            }

            // Info rows
            tvPhone.text = user.phoneNumber.ifBlank { "Not provided" }
            tvCity.text  = user.city.ifBlank { "Location not set" }
            tvVerificationStatus.text = if (user.isVerified) "✅ Email Verified" else "⚠ Email Not Verified"

            // Donor card badge & button
            val isDonor = user.role == UserRole.DONOR
            llVerifiedDonorBadge.isVisible = isDonor
            tvDonorIdProfile.isVisible = isDonor && user.donorId.isNotEmpty()
            btnViewDonorCard.isVisible = isDonor
            if (isDonor && user.donorId.isNotEmpty()) {
                tvDonorIdProfile.text = "Donor ID: ${user.donorId}"
            }

            // Base user specific
            val isBaseUser = user.role == UserRole.USER
            cardCompleteProfile.isVisible = isBaseUser

            // Donor-specific rows
            rowWeight.isVisible       = isDonor
            rowLastDonation.isVisible = isDonor
            cardAvailability.isVisible = isDonor
            cardBadges.isVisible       = isDonor

            if (isDonor) {
                tvWeight.text = user.weightKg?.let { "${it.toInt()} kg" } ?: "Not set"
                tvLastDonation.text = user.lastDonationDate
                    ?.let { "Last donated: ${dateFormatter.format(Date(it))}" }
                    ?: "First-time donor"
                    
                // Availability Toggle
                switchAvailable.isChecked = user.isAvailableToDonate
                
                // Badges (Mock logic for now)
                val donations = 0 // TODO: Real count
                val bronzeColor = if (donations >= 1) requireContext().getColor(R.color.blood_red) else requireContext().getColor(R.color.gray_text)
                val silverColor = if (donations >= 5) requireContext().getColor(R.color.blood_red) else requireContext().getColor(R.color.gray_text)
                val goldColor = if (donations >= 10) requireContext().getColor(R.color.blood_red) else requireContext().getColor(R.color.gray_text)
                
                ivBadgeBronze.imageTintList = ColorStateList.valueOf(bronzeColor)
                ivBadgeSilver.imageTintList = ColorStateList.valueOf(silverColor)
                ivBadgeGold.imageTintList = ColorStateList.valueOf(goldColor)
            }

            // Stats card — role-specific
            if (isDonor) {
                tvStat1Value.text  = "0"   // TODO Step 5: real donation count from Firestore
                tvStat1Label.text  = "Donations"
                tvStat2Value.text  = "0"
                tvStat2Label.text  = "Lives Saved"
                val eligible = user.isDonationEligible
                tvStat3Value.text  = if (eligible) "✓" else "✗"
                tvStat3Value.setTextColor(
                    requireContext().getColor(
                        if (eligible) R.color.success_green
                        else          R.color.error_red
                    )
                )
                tvStat3Label.text  = if (eligible) "Eligible Now" else "Not Eligible"
            } else {
                // Recipient stats
                tvStat1Value.text  = "—"
                tvStat1Label.text  = "Requests"
                tvStat2Value.text  = "—"
                tvStat2Label.text  = "Active"
                tvStat3Value.text  = user.hospitalName.ifBlank { "—" }
                tvStat3Label.text  = "Hospital"
            }
        }
    }
}
