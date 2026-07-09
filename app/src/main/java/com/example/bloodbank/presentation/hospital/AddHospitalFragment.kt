package com.example.bloodbank.presentation.hospital

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
import com.example.bloodbank.R
import com.example.bloodbank.databinding.FragmentAddHospitalBinding
import com.example.bloodbank.presentation.common.extensions.hide
import com.example.bloodbank.presentation.common.extensions.hideKeyboard
import com.example.bloodbank.presentation.common.extensions.show
import com.example.bloodbank.presentation.common.extensions.showErrorSnackbar
import com.example.bloodbank.presentation.common.extensions.showSuccessSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddHospitalFragment : Fragment() {

    private var _binding: FragmentAddHospitalBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddHospitalViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddHospitalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupDropdowns()
        setupClickListeners()
        observeUiState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupDropdowns() {
        val types = listOf("Public (Government)", "Private", "Clinic / Red Cross")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, types)
        binding.acvType.setAdapter(adapter)
    }

    private fun setupClickListeners() {
        binding.btnSaveHospital.setOnClickListener {
            hideKeyboard()
            viewModel.addHospital(
                name = binding.etName.text.toString(),
                address = binding.etAddress.text.toString(),
                city = binding.etCity.text.toString(),
                contact = binding.etContact.text.toString(),
                emergencyContact = binding.etEmergencyContact.text.toString(),
                type = binding.acvType.text.toString(),
                latitudeStr = binding.etLatitude.text.toString(),
                longitudeStr = binding.etLongitude.text.toString()
            )
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is AddHospitalUiState.Idle -> {
                            binding.progressSubmit.hide()
                            binding.btnSaveHospital.isEnabled = true
                        }
                        is AddHospitalUiState.Loading -> {
                            binding.progressSubmit.show()
                            binding.btnSaveHospital.isEnabled = false
                        }
                        is AddHospitalUiState.Success -> {
                            binding.progressSubmit.hide()
                            binding.btnSaveHospital.isEnabled = true
                            
                            // Using a View inside CoordinatorLayout for the snackbar to anchor correctly
                            binding.root.showSuccessSnackbar("Hospital added successfully!")
                            
                            viewModel.resetState()
                            findNavController().navigateUp()
                        }
                        is AddHospitalUiState.Error -> {
                            binding.progressSubmit.hide()
                            binding.btnSaveHospital.isEnabled = true
                            binding.root.showErrorSnackbar(state.message)
                        }
                    }
                }
            }
        }
    }
}
