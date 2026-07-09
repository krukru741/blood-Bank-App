package com.example.bloodbank.presentation.request

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.bloodbank.databinding.FragmentRequestDetailBinding
import com.example.bloodbank.domain.model.BloodRequest
import com.example.bloodbank.domain.model.RequestStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RequestDetailFragment : Fragment() {

    private var _binding: FragmentRequestDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RequestDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRequestDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        val requestId = arguments?.getString("requestId") ?: return

        observeState()
        viewModel.loadRequest(requestId)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is RequestDetailUiState.Loading -> {
                            // Could show progress bar
                        }
                        is RequestDetailUiState.Success -> {
                            bindRequestDetails(state.request)
                        }
                        is RequestDetailUiState.Error -> {
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun bindRequestDetails(request: BloodRequest) {
        binding.tvBloodType.text = request.bloodType.label
        binding.tvPatientName.text = request.requesterName
        binding.tvHospital.text = "📍 ${request.hospital}"
        
        binding.btnGetDirections.setOnClickListener {
            if (request.latitude != null && request.longitude != null) {
                val gmmIntentUri = android.net.Uri.parse("google.navigation:q=${request.latitude},${request.longitude}")
                val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
                    startActivity(mapIntent)
                } else {
                    Toast.makeText(requireContext(), "Google Maps app not found", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Location not available", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.tvDescription.text = request.description
        binding.tvUnits.text = "${request.unitsNeeded} Units"
        
        val isOwner = (request.requesterId == viewModel.currentUserId)
        
        // Show edit button if owner and status is PENDING
        if (isOwner && request.status == RequestStatus.PENDING) {
            binding.btnEditRequest.visibility = View.VISIBLE
            binding.btnEditRequest.setOnClickListener {
                val action = RequestDetailFragmentDirections.actionRequestDetailToEditRequest(request.requestId)
                findNavController().navigate(action)
            }
        } else {
            binding.btnEditRequest.visibility = View.GONE
        }
        
        val urgencyColor = if (request.urgency == com.example.bloodbank.domain.model.UrgencyLevel.CRITICAL) {
            "#D32F2F" // Red
        } else {
            "#F57C00" // Orange
        }
        binding.tvUrgency.text = "⚠️ ${request.urgency.name}"
        binding.tvUrgency.setTextColor(android.graphics.Color.parseColor(urgencyColor))

        // UI Logic for Donor vs Recipient Response
        binding.btnAcceptRequest.visibility = View.GONE
        binding.layoutContactDonor.visibility = View.GONE
        binding.tvMismatchWarning.visibility = View.GONE
        binding.btnShareRequest.visibility = View.GONE
        
        when (request.status) {
            RequestStatus.PENDING -> {
                if (!isOwner) {
                    val currentState = viewModel.uiState.value
                    val currentUser = if (currentState is RequestDetailUiState.Success) currentState.currentUser else null
                    
                    val isMatch = if (currentUser != null) {
                        com.example.bloodbank.domain.util.BloodCompatibility.isMatch(currentUser.bloodType, request.bloodType)
                    } else {
                        false
                    }

                    if (isMatch) {
                        binding.btnAcceptRequest.visibility = View.VISIBLE
                        binding.btnAcceptRequest.setOnClickListener {
                            showAcceptConfirmationDialog(request)
                        }
                    } else {
                        binding.tvMismatchWarning.visibility = View.VISIBLE
                        val donorTypeLabel = currentUser?.bloodType?.label ?: "Unknown"
                        binding.tvMismatchWarning.text = "⚠️ Your blood type ($donorTypeLabel) does not match this patient."
                        
                        binding.btnShareRequest.visibility = View.VISIBLE
                        binding.btnShareRequest.text = "📢 SHARE THIS REQUEST"
                        binding.btnShareRequest.setOnClickListener {
                            shareRequest(request)
                        }
                    }
                } else {
                    // Owner can share their own request
                    binding.btnShareRequest.visibility = View.VISIBLE
                    binding.btnShareRequest.text = "📢 SHARE YOUR REQUEST"
                    binding.btnShareRequest.setOnClickListener {
                        shareRequest(request)
                    }
                }
            }
            RequestStatus.MATCHED -> {
                binding.layoutContactDonor.visibility = View.VISIBLE
                
                if (isOwner) {
                    binding.tvMatchedStatus.text = "Matched with Donor: ${request.acceptedByDonorName}"
                } else if (request.acceptedByDonorId == viewModel.currentUserId) {
                    binding.tvMatchedStatus.text = "You accepted this request!"
                } else {
                    binding.tvMatchedStatus.text = "This request has already been accepted by someone else."
                    binding.btnCallDonor.visibility = View.GONE
                    binding.btnTextDonor.visibility = View.GONE
                }

                val contactNumber = if (isOwner) request.acceptedByDonorPhone else request.contactNumber

                if (contactNumber != null && (isOwner || request.acceptedByDonorId == viewModel.currentUserId)) {
                    binding.btnCallDonor.visibility = View.VISIBLE
                    binding.btnTextDonor.visibility = View.VISIBLE
                    
                    binding.btnCallDonor.setOnClickListener {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:$contactNumber")
                        }
                        startActivity(intent)
                    }
                    
                    binding.btnTextDonor.setOnClickListener {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("smsto:$contactNumber")
                        }
                        startActivity(intent)
                    }
                }
            }
            else -> {
                // Handle FULFILLED, EXPIRED, CANCELLED
            }
        }
    }

    private fun showAcceptConfirmationDialog(request: BloodRequest) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Donation")
            .setMessage("Sigurado ka ba nga mo-donate ka niini nga pasyente? Ihatag ang imong contact number para matawagan ka sa pasyente.")
            .setPositiveButton("Yes, I want to donate") { _, _ ->
                viewModel.acceptRequest(request)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun shareRequest(request: BloodRequest) {
        val shareText = "URGENT: ${request.requesterName} needs ${request.unitsNeeded} units of ${request.bloodType.label} blood at ${request.hospital}. Please download the BloodBank app to help!"
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Request")
        startActivity(shareIntent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
