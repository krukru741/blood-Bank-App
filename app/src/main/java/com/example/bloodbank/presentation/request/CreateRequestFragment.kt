package com.example.bloodbank.presentation.request

import android.annotation.SuppressLint
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.bloodbank.R
import com.example.bloodbank.databinding.FragmentCreateRequestBinding
import com.example.bloodbank.domain.model.UrgencyLevel
import com.example.bloodbank.presentation.common.extensions.hide
import com.example.bloodbank.presentation.common.extensions.hideKeyboard
import com.example.bloodbank.presentation.common.extensions.show
import com.example.bloodbank.presentation.common.extensions.showErrorSnackbar
import com.example.bloodbank.presentation.common.extensions.showSuccessSnackbar
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

@AndroidEntryPoint
class CreateRequestFragment : Fragment() {

    private var _binding: FragmentCreateRequestBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreateRequestViewModel by viewModels()

    private var currentStep = 0

    private var currentLat: Double? = null
    private var currentLng: Double? = null
    
    private var pickedLat: Double? = null
    private var pickedLng: Double? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (fineGranted || coarseGranted) {
                getCurrentLocation()
            } else {
                binding.coordinatorCreateRequest.showErrorSnackbar("Location permission is required to use GPS.")
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateRequestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        setupBloodTypeDropdown()
        preFillFromProfile()
        setupStepper()
        setupMapPicker()
        setupLocationButtons()
        setupPsgcListeners()
        setupErrorClearing()
        setupRetryListeners()
        observeState()
        
        updateStepperUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Stepper Logic ─────────────────────────────────────────────────────────

    private fun setupStepper() {
        binding.btnNext.setOnClickListener {
            if (validateCurrentStep()) {
                if (currentStep < 2) {
                    currentStep++
                    updateStepperUI()
                } else {
                    binding.btnNext.isEnabled = false // prevent double click
                    submitRequest()
                }
            }
        }

        binding.btnBack.setOnClickListener {
            if (currentStep > 0) {
                currentStep--
                updateStepperUI()
            }
        }
    }

    private fun updateStepperUI() {
        binding.viewFlipperSteps.displayedChild = currentStep
        
        val activeColor = requireContext().getColor(R.color.blood_red)
        val inactiveColor = requireContext().getColor(R.color.gray_text)
        
        binding.tvStep1.setTextColor(if (currentStep >= 0) activeColor else inactiveColor)
        binding.tvStep2.setTextColor(if (currentStep >= 1) activeColor else inactiveColor)
        binding.tvStep3.setTextColor(if (currentStep >= 2) activeColor else inactiveColor)

        if (currentStep == 0) {
            binding.btnBack.visibility = View.INVISIBLE
            binding.btnNext.text = "Next"
        } else if (currentStep == 1) {
            binding.btnBack.visibility = View.VISIBLE
            binding.btnNext.text = "Next"
        } else {
            binding.btnBack.visibility = View.VISIBLE
            binding.btnNext.text = "Post Request"
        }
    }

    private fun validateCurrentStep(): Boolean {
        return when (currentStep) {
            0 -> viewModel.validateStep1(
                bloodType = binding.acvBloodType.text.toString().trim(),
                units = binding.etUnits.text.toString().trim()
            )
            1 -> viewModel.validateStep2(
                hospital = binding.etHospital.text.toString().trim(),
                province = binding.etProvince.text.toString().trim(),
                city = binding.etCity.text.toString().trim()
            )
            2 -> viewModel.validateStep3(
                contact = binding.etContact.text.toString().trim()
            )
            else -> false
        }
    }

    private fun setupErrorClearing() {
        binding.acvBloodType.doOnTextChanged { _, _, _, _ -> viewModel.clearError(FormField.BLOOD_TYPE) }
        binding.etUnits.doOnTextChanged { _, _, _, _ -> viewModel.clearError(FormField.UNITS) }
        binding.etHospital.doOnTextChanged { _, _, _, _ -> viewModel.clearError(FormField.HOSPITAL) }
        binding.etProvince.doOnTextChanged { _, _, _, _ -> viewModel.clearError(FormField.PROVINCE) }
        binding.etCity.doOnTextChanged { _, _, _, _ -> viewModel.clearError(FormField.CITY) }
        binding.etContact.doOnTextChanged { _, _, _, _ -> viewModel.clearError(FormField.CONTACT) }
    }

    private fun setupRetryListeners() {
        binding.btnRetryPsgc.setOnClickListener {
            binding.layoutPsgcError.isVisible = false
            viewModel.fetchProvinces()
        }
    }

    // ── Dropdowns and Pre-fill ──────────────────────────────────────────────

    private fun setupBloodTypeDropdown() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            viewModel.bloodTypeOptions
        )
        binding.acvBloodType.setAdapter(adapter)
    }

    private fun preFillFromProfile() {
        val prefill = viewModel.prefill
        binding.acvBloodType.setText(prefill.bloodType, false)
        binding.etHospital.setText(prefill.hospitalName)
        binding.etContact.setText(prefill.phone)
    }

    // ── Location & Map Picker ────────────────────────────────────────────────

    private fun setupLocationButtons() {
        binding.btnUseLocation.setOnClickListener {
            requestPermissionLauncher.launch(
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }

        binding.btnPickMap.setOnClickListener {
            binding.layoutMapPicker.isVisible = true
            binding.webViewMapPicker.evaluateJavascript("javascript:enablePickerMode();", null)
        }

        binding.btnCloseMap.setOnClickListener {
            binding.layoutMapPicker.isVisible = false
        }

        binding.btnConfirmLocation.setOnClickListener {
            if (pickedLat != null && pickedLng != null) {
                currentLat = pickedLat
                currentLng = pickedLng
                binding.layoutMapPicker.isVisible = false
                reverseGeocodeLocation(pickedLat!!, pickedLng!!)
            } else {
                binding.coordinatorCreateRequest.showErrorSnackbar("Please tap on the map to pick a location.")
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupMapPicker() {
        binding.webViewMapPicker.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
            addJavascriptInterface(MapPickerJavascriptInterface(), "Android")
            loadUrl("file:///android_asset/map.html")
        }
    }

    inner class MapPickerJavascriptInterface {
        @JavascriptInterface
        fun onLocationPicked(lat: Double, lng: Double) {
            pickedLat = lat
            pickedLng = lng
        }
        
        @JavascriptInterface
        fun onMarkerClick(requestId: String) {
            // Unused in picker mode
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        binding.btnUseLocation.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val location = fusedLocationClient.lastLocation.await()
                binding.btnUseLocation.isEnabled = true
                if (location != null) {
                    currentLat = location.latitude
                    currentLng = location.longitude
                    reverseGeocodeLocation(location.latitude, location.longitude)
                } else {
                    binding.coordinatorCreateRequest.showErrorSnackbar("Could not get current location.")
                }
            } catch (e: Exception) {
                binding.btnUseLocation.isEnabled = true
                binding.coordinatorCreateRequest.showErrorSnackbar("Failed to get location: ${e.localizedMessage}")
            }
        }
    }
    
    private fun reverseGeocodeLocation(lat: Double, lng: Double) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(lat, lng, 1) { addresses ->
                    val address = addresses.firstOrNull()
                    if (address != null) {
                        requireActivity().runOnUiThread { fillLocationFields(address) }
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                val address = addresses?.firstOrNull()
                if (address != null) {
                    fillLocationFields(address)
                }
            }
        } catch (e: Exception) {
            binding.coordinatorCreateRequest.showErrorSnackbar("Failed to get address from coordinates.")
        }
    }

    private fun fillLocationFields(address: android.location.Address) {
        binding.etProvince.setText(address.adminArea ?: "", false)
        binding.etCity.setText(address.locality ?: address.subAdminArea ?: "", false)
        binding.etBarangay.setText(address.subLocality ?: "", false)
        val street = listOfNotNull(address.thoroughfare, address.subThoroughfare).joinToString(" ")
        binding.etStreet.setText(street)
        binding.coordinatorCreateRequest.showSuccessSnackbar("Address auto-filled!")
    }

    // ── PSGC Flow & UI State Observation ────────────────────────────────────────────────────────────

    private fun setupPsgcListeners() {
        binding.etProvince.setOnItemClickListener { _, _, _, _ ->
            val selectedName = binding.etProvince.text.toString()
            val province = viewModel.uiState.value.provinces.find { it.name == selectedName }
            province?.let {
                binding.etCity.setText("", false)
                binding.etBarangay.setText("", false)
                viewModel.fetchCities(it.code)
            }
        }
        binding.etCity.setOnItemClickListener { _, _, _, _ ->
            val selectedName = binding.etCity.text.toString()
            val city = viewModel.uiState.value.cities.find { it.name == selectedName }
            city?.let {
                binding.etBarangay.setText("", false)
                viewModel.fetchBarangays(it.code)
            }
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Dropdowns
                    if (binding.etProvince.adapter?.count != state.provinces.size) {
                        binding.etProvince.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, state.provinces.map { it.name }))
                    }
                    if (binding.etCity.adapter?.count != state.cities.size) {
                        binding.etCity.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, state.cities.map { it.name }))
                    }
                    if (binding.etBarangay.adapter?.count != state.barangays.size) {
                        binding.etBarangay.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, state.barangays.map { it.name }))
                    }

                    // Form Errors
                    binding.tilBloodType.error = state.formErrors.bloodType
                    binding.tilUnits.error = state.formErrors.units
                    binding.tilHospital.error = state.formErrors.hospital
                    binding.tilProvince.error = state.formErrors.province
                    binding.tilCity.error = state.formErrors.city
                    binding.tilContact.error = state.formErrors.contact

                    // PSGC Offline state
                    if (state.psgcError != null) {
                        binding.layoutPsgcError.isVisible = true
                        binding.tvPsgcErrorMsg.text = state.psgcError
                    } else {
                        binding.layoutPsgcError.isVisible = false
                    }

                    // Loading State
                    if (state.isLoading) {
                        setFormEnabled(false)
                        binding.progressSubmit.show()
                    } else {
                        binding.progressSubmit.hide()
                        // Ensure form is re-enabled if loading finished but not successfully navigating away
                        if (!state.isSuccess) setFormEnabled(true)
                    }

                    // Submission Result
                    if (state.isSuccess) {
                        findNavController().navigateUp()
                        requireActivity().window.decorView.showSuccessSnackbar(
                            "🩸 Blood request posted successfully!"
                        )
                        viewModel.resetState()
                    }

                    if (state.errorMessage != null) {
                        setFormEnabled(true)
                        binding.coordinatorCreateRequest.showErrorSnackbar(state.errorMessage)
                        viewModel.resetState()
                    }
                }
            }
        }
    }

    // ── Submission & State ───────────────────────────────────────────────────

    private fun submitRequest() {
        hideKeyboard()
        viewModel.submitRequest(
            bloodTypeLabel = binding.acvBloodType.text.toString().trim(),
            units          = binding.etUnits.text.toString().trim(),
            hospital       = binding.etHospital.text.toString().trim(),
            province       = binding.etProvince.text.toString().trim(),
            city           = binding.etCity.text.toString().trim(),
            barangay       = binding.etBarangay.text.toString().trim(),
            street         = binding.etStreet.text.toString().trim(),
            latitude       = currentLat,
            longitude      = currentLng,
            contact        = binding.etContact.text.toString().trim(),
            notes          = binding.etNotes.text.toString().trim(),
            urgency        = selectedUrgency()
        )
    }

    private fun selectedUrgency(): UrgencyLevel = when {
        binding.chipCritical.isChecked -> UrgencyLevel.CRITICAL
        binding.chipUrgent.isChecked   -> UrgencyLevel.URGENT
        else                           -> UrgencyLevel.NORMAL
    }

    private fun setFormEnabled(enabled: Boolean) {
        with(binding) {
            acvBloodType.isEnabled      = enabled
            etUnits.isEnabled           = enabled
            etHospital.isEnabled        = enabled
            etProvince.isEnabled        = enabled
            etCity.isEnabled            = enabled
            etBarangay.isEnabled        = enabled
            etStreet.isEnabled          = enabled
            etContact.isEnabled         = enabled
            etNotes.isEnabled           = enabled
            chipNormal.isEnabled        = enabled
            chipUrgent.isEnabled        = enabled
            chipCritical.isEnabled      = enabled
            btnNext.isEnabled           = enabled
            btnBack.isEnabled           = enabled
            btnUseLocation.isEnabled    = enabled
            btnPickMap.isEnabled        = enabled
        }
    }
}
