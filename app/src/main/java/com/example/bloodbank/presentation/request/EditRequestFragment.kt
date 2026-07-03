package com.example.bloodbank.presentation.request

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.bloodbank.R
import com.example.bloodbank.databinding.FragmentEditRequestBinding
import com.example.bloodbank.domain.model.BloodRequest
import com.example.bloodbank.domain.model.UrgencyLevel
import com.example.bloodbank.presentation.common.extensions.hide
import com.example.bloodbank.presentation.common.extensions.hideKeyboard
import com.example.bloodbank.presentation.common.extensions.show
import com.example.bloodbank.presentation.common.extensions.showErrorSnackbar
import com.example.bloodbank.presentation.common.extensions.showSuccessSnackbar
import com.example.bloodbank.presentation.common.extensions.text
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditRequestFragment : Fragment() {

    private var _binding: FragmentEditRequestBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditRequestViewModel by viewModels()

    // Using navArgs to get the request ID passed from RequestDetailFragment
    private val args: EditRequestFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditRequestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBloodTypeDropdown()
        setupSubmitButton()
        observeUiState()

        // Load the request details based on ID
        if (savedInstanceState == null) {
            viewModel.loadRequest(args.requestId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Setup ──────────────────────────────────────────────────────────────────

    private fun setupBloodTypeDropdown() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            viewModel.bloodTypeOptions
        )
        binding.acvBloodType.setAdapter(adapter)
    }

    private fun populateForm(request: BloodRequest) {
        binding.acvBloodType.setText(request.bloodType.label, false)
        binding.etUnits.setText(request.unitsNeeded.toString())
        binding.etHospital.setText(request.hospital)
        binding.etLocation.setText(request.location)
        binding.etContact.setText(request.contactNumber)
        binding.etNotes.setText(request.description)

        when (request.urgency) {
            UrgencyLevel.NORMAL -> binding.chipNormal.isChecked = true
            UrgencyLevel.URGENT -> binding.chipUrgent.isChecked = true
            UrgencyLevel.CRITICAL -> binding.chipCritical.isChecked = true
        }
    }

    private fun setupSubmitButton() {
        binding.btnUpdateRequest.setOnClickListener {
            hideKeyboard()
            viewModel.updateRequest(
                bloodTypeLabel = binding.acvBloodType.text.toString(),
                units          = binding.tilUnits.text,
                hospital       = binding.tilHospital.text,
                location       = binding.tilLocation.text,
                contact        = binding.tilContact.text,
                notes          = binding.tilNotes.text,
                urgency        = selectedUrgency()
            )
        }
    }

    private fun selectedUrgency(): UrgencyLevel = when {
        binding.chipCritical.isChecked -> UrgencyLevel.CRITICAL
        binding.chipUrgent.isChecked -> UrgencyLevel.URGENT
        else -> UrgencyLevel.NORMAL
    }

    // ── Observation ────────────────────────────────────────────────────────────

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is EditRequestUiState.Idle -> {
                            binding.progressSubmit.hide()
                            binding.btnUpdateRequest.isEnabled = true
                        }
                        is EditRequestUiState.Loading -> {
                            binding.progressSubmit.show()
                            binding.btnUpdateRequest.isEnabled = false
                        }
                        is EditRequestUiState.Loaded -> {
                            binding.progressSubmit.hide()
                            binding.btnUpdateRequest.isEnabled = true
                            populateForm(state.request)
                        }
                        is EditRequestUiState.Updated -> {
                            binding.progressSubmit.hide()
                            binding.btnUpdateRequest.isEnabled = true
                            binding.coordinatorEditRequest.showSuccessSnackbar("Request updated successfully!")
                            findNavController().navigateUp()
                            viewModel.resetState()
                        }
                        is EditRequestUiState.Error -> {
                            binding.progressSubmit.hide()
                            binding.btnUpdateRequest.isEnabled = true
                            binding.coordinatorEditRequest.showErrorSnackbar(state.message)
                        }
                    }
                }
            }
        }
    }
}
