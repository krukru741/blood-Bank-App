package com.example.bloodbank.presentation.profile

import android.annotation.SuppressLint
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.bloodbank.databinding.FragmentBecomeDonorBinding
import com.example.bloodbank.presentation.common.AuthUiState
import com.example.bloodbank.presentation.common.extensions.clearError
import com.example.bloodbank.presentation.common.extensions.hide
import com.example.bloodbank.presentation.common.extensions.hideKeyboard
import com.example.bloodbank.presentation.common.extensions.show
import com.example.bloodbank.presentation.common.extensions.showErrorSnackbar
import com.example.bloodbank.presentation.common.extensions.showSuccessSnackbar
import com.example.bloodbank.presentation.common.extensions.text
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class BecomeDonorFragment : Fragment() {

    private var _binding: FragmentBecomeDonorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BecomeDonorViewModel by viewModels()

    private var selectedDob: Long? = null
    private var selectedLastDonationDate: Long? = null
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    private var currentLat: Double? = null
    private var currentLng: Double? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (fineGranted || coarseGranted) {
                getCurrentLocation()
            } else {
                binding.coordinatorBecomeDonor.showErrorSnackbar("Location permission is required to use GPS.")
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBecomeDonorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupToolbar()
        setupDropdowns()
        setupDatePickers()
        setupTextChangeListeners()
        setupPsgcListeners()
        setupButtonListeners()
        observeUiState()
        observePsgc()
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
        ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, viewModel.bloodTypeOptions)
            .also { binding.acvBloodType.setAdapter(it) }
        binding.acvBloodType.setText(viewModel.bloodTypeOptions.first(), false)

        ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, viewModel.genderOptions)
            .also { binding.acvGender.setAdapter(it) }
        binding.acvGender.setText(viewModel.genderOptions.first(), false)
    }

    private fun setupDatePickers() {
        binding.etDob.setOnClickListener { showDobPicker() }
        binding.tilDob.setEndIconOnClickListener { showDobPicker() }

        binding.etLastDonation.setOnClickListener { showLastDonationPicker() }
        binding.tilLastDonation.setEndIconOnClickListener { showLastDonationPicker() }

        binding.btnClearLastDonation.setOnClickListener {
            selectedLastDonationDate = null
            binding.etLastDonation.setText("")
            binding.btnClearLastDonation.hide()
        }
    }

    private fun showDobPicker() {
        val maxDob = Calendar.getInstance().apply { add(Calendar.YEAR, -18) }.timeInMillis
        val constraints = CalendarConstraints.Builder().setValidator(DateValidatorPointBackward.before(maxDob)).setEnd(maxDob).build()
        val picker = MaterialDatePicker.Builder.datePicker().setTitleText("Select Date of Birth")
            .setSelection(selectedDob ?: maxDob).setCalendarConstraints(constraints).build()

        picker.addOnPositiveButtonClickListener { epochMs ->
            selectedDob = epochMs
            binding.etDob.setText(dateFormatter.format(Date(epochMs)))
            binding.tilDob.clearError()
        }
        picker.show(parentFragmentManager, "dob")
    }

    private fun showLastDonationPicker() {
        val constraints = CalendarConstraints.Builder().setValidator(DateValidatorPointBackward.now()).build()
        val picker = MaterialDatePicker.Builder.datePicker().setTitleText("Last Donation Date")
            .setSelection(selectedLastDonationDate ?: MaterialDatePicker.todayInUtcMilliseconds()).setCalendarConstraints(constraints).build()

        picker.addOnPositiveButtonClickListener { epochMs ->
            selectedLastDonationDate = epochMs
            binding.etLastDonation.setText(dateFormatter.format(Date(epochMs)))
            binding.tilLastDonation.clearError()
            binding.btnClearLastDonation.show()
        }
        picker.show(parentFragmentManager, "last_don")
    }

    private fun setupTextChangeListeners() {
        with(binding) {
            etCity.addTextChangedListener            { tilCity.clearError() }
            etWeight.addTextChangedListener          { tilWeight.clearError() }
        }
    }

    private fun setupButtonListeners() {
        binding.btnUseLocation.setOnClickListener {
            requestPermissionLauncher.launch(
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }

        binding.btnSave.setOnClickListener {
            hideKeyboard()
            viewModel.saveDonorProfile(
                bloodTypeLabel   = binding.acvBloodType.text.toString(),
                genderLabel      = binding.acvGender.text.toString(),
                dateOfBirth      = selectedDob,
                weightText       = binding.tilWeight.text,
                lastDonationDate = selectedLastDonationDate,
                province         = binding.tilProvince.text,
                city             = binding.tilCity.text,
                barangay         = binding.tilBarangay.text,
                street           = binding.tilStreet.text,
                latitude         = currentLat,
                longitude        = currentLng
            )
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is AuthUiState.Idle    -> setFormEnabled(true)
                        is AuthUiState.Loading -> {
                            setFormEnabled(false)
                            binding.progressLoading.show()
                        }
                        is AuthUiState.Success -> {
                            binding.progressLoading.hide()
                            findNavController().navigateUp()
                            requireActivity().window.decorView.showSuccessSnackbar("🎉 You are now a registered Blood Donor!")
                        }
                        is AuthUiState.Error   -> {
                            binding.progressLoading.hide()
                            setFormEnabled(true)
                            binding.coordinatorBecomeDonor.showErrorSnackbar(state.message)
                            viewModel.resetState()
                        }
                    }
                }
            }
        }
    }

    private fun observePsgc() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.provinces.collect { list ->
                        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, list.map { it.name })
                        binding.etProvince.setAdapter(adapter)
                    }
                }
                launch {
                    viewModel.cities.collect { list ->
                        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, list.map { it.name })
                        binding.etCity.setAdapter(adapter)
                    }
                }
                launch {
                    viewModel.barangays.collect { list ->
                        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, list.map { it.name })
                        binding.etBarangay.setAdapter(adapter)
                    }
                }
            }
        }
    }

    private fun setupPsgcListeners() {
        binding.etProvince.setOnItemClickListener { _, _, _, _ ->
            val selectedName = binding.etProvince.text.toString()
            val province = viewModel.provinces.value.find { it.name == selectedName }
            province?.let {
                binding.etCity.setText("", false)
                binding.etBarangay.setText("", false)
                viewModel.fetchCities(it.code)
            }
        }
        binding.etCity.setOnItemClickListener { _, _, _, _ ->
            val selectedName = binding.etCity.text.toString()
            val city = viewModel.cities.value.find { it.name == selectedName }
            city?.let {
                binding.etBarangay.setText("", false)
                viewModel.fetchBarangays(it.code)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        binding.progressLoading.show()
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            binding.progressLoading.hide()
            if (location != null) {
                currentLat = location.latitude
                currentLng = location.longitude
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                            val address = addresses.firstOrNull()
                            if (address != null) {
                                requireActivity().runOnUiThread { fillLocationFields(address) }
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        val address = addresses?.firstOrNull()
                        if (address != null) {
                            fillLocationFields(address)
                        }
                    }
                } catch (e: Exception) {
                    binding.coordinatorBecomeDonor.showErrorSnackbar("Failed to get address from coordinates.")
                }
            } else {
                binding.coordinatorBecomeDonor.showErrorSnackbar("Could not get current location.")
            }
        }.addOnFailureListener {
            binding.progressLoading.hide()
            binding.coordinatorBecomeDonor.showErrorSnackbar("Failed to get location.")
        }
    }

    private fun fillLocationFields(address: android.location.Address) {
        binding.etProvince.setText(address.adminArea ?: "", false)
        binding.etCity.setText(address.locality ?: address.subAdminArea ?: "", false)
        binding.etBarangay.setText(address.subLocality ?: "", false)
        val street = listOfNotNull(address.thoroughfare, address.subThoroughfare).joinToString(" ")
        binding.etStreet.setText(street)
    }

    private fun setFormEnabled(enabled: Boolean) {
        with(binding) {
            acvBloodType.isEnabled        = enabled
            etProvince.isEnabled          = enabled
            etCity.isEnabled              = enabled
            etBarangay.isEnabled          = enabled
            etStreet.isEnabled            = enabled
            acvGender.isEnabled           = enabled
            etDob.isEnabled               = enabled
            etWeight.isEnabled            = enabled
            etLastDonation.isEnabled      = enabled
            btnSave.isEnabled             = enabled
        }
    }
}
