package com.example.bloodbank.presentation.home

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
import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebViewClient
import org.json.JSONArray
import org.json.JSONObject
import androidx.navigation.fragment.findNavController
import com.example.bloodbank.R
import com.example.bloodbank.databinding.FragmentHomeBinding
import com.example.bloodbank.domain.model.BloodRequest
import com.example.bloodbank.domain.repository.AuthRepository
import com.example.bloodbank.presentation.common.extensions.showErrorSnackbar
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.Manifest
import com.bumptech.glide.Glide
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

/**
 * HomeFragment — Blood Request Feed (Map View Only)
 */
@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var adapter: BloodRequestAdapter

    @Inject lateinit var authRepository: AuthRepository

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                fetchCurrentLocation()
            }
            else -> {
                binding.coordinatorHome.showErrorSnackbar("Location permission denied. Using default map view.")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        arguments?.getString("initialFilter")?.let { filter ->
            if (filter == "HOSPITALS") {
                viewModel.setFilter(FeedFilter.HOSPITALS)
            }
        }
        
        setupFab()
        setupWebView()
        setupRecyclerView()
        setupToggleAndFilters()
        observeUiState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── FAB ────────────────────────────────────────────────────────────────────

    private fun setupFab() {
        binding.fabCreateRequest.setOnClickListener {
            val options = arrayOf("🩸 I want to Donate Blood", "🏥 I need Blood (Create Request)")
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("What would you like to do?")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> findNavController().navigate(R.id.action_home_to_become_donor)
                        1 -> findNavController().navigate(R.id.action_home_to_create_request)
                    }
                }
                .show()
        }

        binding.fabMyLocation.setOnClickListener {
            val hasFine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            
            if (hasFine || hasCoarse) {
                // Permission already granted, fetch location
                fetchCurrentLocation()
            } else {
                // Request permissions
                locationPermissionRequest.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
        }

        binding.fabFindHospital.setOnClickListener {
            // 1. Switch to Map mode via the toggle group
            binding.toggleView.check(R.id.btn_map)
            
            // 2. Hide any open popups
            binding.cardRequestPopup.isVisible = false
            
            // 3. Set the filter to HOSPITALS
            viewModel.setFilter(FeedFilter.HOSPITALS)
            
            // 4. Fly to user's saved location if available, otherwise zoom out to whole country
            val userLoc = viewModel.uiState.value.userLocation
            if (userLoc != null) {
                binding.webViewMap.evaluateJavascript(
                    "javascript:map.flyTo([${userLoc.first}, ${userLoc.second}], 12, {animate: true, duration: 1});",
                    null
                )
            } else {
                binding.webViewMap.evaluateJavascript(
                    "javascript:map.flyTo([12.8797, 121.7740], 6, {animate: true, duration: 1});",
                    null
                )
            }
            
            com.google.android.material.snackbar.Snackbar.make(
                binding.root,
                "🏥 Showing hospitals & blood centers near you",
                com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
            ).setBackgroundTint(android.graphics.Color.parseColor("#1976D2"))
             .setTextColor(android.graphics.Color.WHITE)
             .show()
        }
    }


    // ── List View & Filters ───────────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = BloodRequestAdapter(
            onRespondClick = { navigateToDetail(it.requestId) },
            onCardClick = { navigateToDetail(it.requestId) }
        )
        binding.rvListView.adapter = adapter
        binding.rvListView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
    }

    private fun navigateToDetail(requestId: String) {
        val bundle = Bundle().apply { putString("requestId", requestId) }
        findNavController().navigate(R.id.action_home_to_request_detail, bundle)
    }

    private fun setupToggleAndFilters() {
        // Toggle Map / List
        binding.toggleView.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val isMap = checkedId == R.id.btn_map
                binding.webViewMap.isVisible = isMap
                binding.cardLegend.isVisible = isMap
                binding.rvListView.isVisible = !isMap
                
                // Hide popup if switching to list
                if (!isMap) binding.cardRequestPopup.isVisible = false
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocation() {
        com.google.android.material.snackbar.Snackbar.make(binding.root, "📍 Fetching location...", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    val myLat = location.latitude
                    val myLng = location.longitude
                    
                    viewModel.updateUserLocation(myLat, myLng)
                    binding.toggleView.check(R.id.btn_map)
                    
                    binding.webViewMap.evaluateJavascript(
                        "javascript:map.flyTo([$myLat, $myLng], 14, {animate: true});", null
                    )
                    
                    com.google.android.material.snackbar.Snackbar.make(
                        binding.root, "📍 Location saved & updated",
                        com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    binding.coordinatorHome.showErrorSnackbar("Could not detect current location.")
                }
            }
            .addOnFailureListener {
                binding.coordinatorHome.showErrorSnackbar("Error fetching location.")
            }
    }

    // ── Map Setup ──────────────────────────────────────────────────────────────
    
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webViewMap.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            addJavascriptInterface(MapJavascriptInterface(), "Android")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: android.webkit.WebView, url: String) {
                    super.onPageFinished(view, url)
                    // Push current markers after map.html is fully loaded
                    val state = viewModel.uiState.value
                    updateMapMarkers(state.filteredRequests, state.hospitals)
                }
            }
            loadUrl("file:///android_asset/map.html")
        }
    }

    inner class MapJavascriptInterface {
        @JavascriptInterface
        fun onMarkerClick(id: String) {
            requireActivity().runOnUiThread {
                if (id == "my_loc") {
                    showMyLocationPopup()
                } else if (id.startsWith("h")) {
                    val hospital = viewModel.uiState.value.hospitals.find { it.id == id }
                    if (hospital != null) showHospitalPopup(hospital)
                } else {
                    val request = viewModel.uiState.value.filteredRequests.find { it.requestId == id }
                    if (request != null) showRequestPopup(request)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showMyLocationPopup() {
        binding.cardRequestPopup.isVisible = true
        binding.tvPopupName.text = "You are here"
        binding.tvPopupRole.text = "Current Location"
        binding.tvPopupBloodType.text = "📍"
        binding.tvPopupLocation.text = "Your detected GPS location."
        
        binding.btnPopupAction.text = "Search Nearby Hospitals"
        
        val userLoc = viewModel.uiState.value.userLocation
        if (userLoc != null) {
            binding.webViewMap.evaluateJavascript(
                "javascript:map.flyTo([${userLoc.first}, ${userLoc.second}], 14, {animate: true});", null
            )
        }

        binding.btnPopupClose.setOnClickListener {
            binding.cardRequestPopup.isVisible = false
        }

        binding.btnPopupAction.setOnClickListener {
            binding.cardRequestPopup.isVisible = false
            // Act like the Find Hospital FAB was clicked
            binding.fabFindHospital.performClick()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showHospitalPopup(hospital: com.example.bloodbank.domain.model.HospitalMarker) {
        binding.cardRequestPopup.isVisible = true
        
        // Hide request specific fields
        binding.llPopupBloodType.isVisible = false
        binding.ivPopupAvatar.isVisible = false // Hide profile for hospitals
        
        // Populate Header
        binding.tvPopupName.text = hospital.name
        binding.tvPopupRole.text = "🏥 Hospital / Blood Center"
        binding.tvPopupLocation.text = "${hospital.address}"
        
        // Hospital Banner Image
        if (!hospital.imageUrl.isNullOrEmpty()) {
            binding.ivPopupBanner.isVisible = true
            Glide.with(requireContext())
                .load(hospital.imageUrl)
                .placeholder(R.drawable.ic_menu_hospital)
                .error(R.drawable.ic_menu_hospital)
                .centerCrop()
                .into(binding.ivPopupBanner)
        } else {
            binding.ivPopupBanner.isVisible = false
        }
        
        // Contact Info
        if (!hospital.contactNumber.isNullOrEmpty()) {
            binding.llPopupContact.isVisible = true
            binding.tvPopupContact.text = hospital.contactNumber
        } else {
            binding.llPopupContact.isVisible = false
        }
        
        // Distance Calculation
        val userLoc = viewModel.uiState.value.userLocation
        if (userLoc != null) {
            val distKm = calculateDistance(userLoc.first, userLoc.second, hospital.latitude, hospital.longitude)
            binding.llPopupDistance.isVisible = true
            binding.tvPopupDistance.text = String.format("%.1f km away", distKm)
        } else {
            binding.llPopupDistance.isVisible = false
        }

        binding.btnPopupAction.text = "Get Directions"

        // Zoom map to the pin
        binding.webViewMap.evaluateJavascript(
            "javascript:map.flyTo([${hospital.latitude}, ${hospital.longitude}], 14, {animate: true});",
            null
        )

        binding.btnPopupClose.setOnClickListener {
            binding.cardRequestPopup.isVisible = false
        }

        binding.btnPopupAction.setOnClickListener {
            val gmmIntentUri = android.net.Uri.parse("geo:0,0?q=${android.net.Uri.encode(hospital.name)}")
            val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(mapIntent)
            } else {
                binding.coordinatorHome.showErrorSnackbar("Google Maps app not found")
            }
            binding.cardRequestPopup.isVisible = false
        }
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371 // Earth's radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c // Distance in km
    }

    @SuppressLint("SetTextI18n")
    private fun showRequestPopup(request: com.example.bloodbank.domain.model.BloodRequest) {
        binding.cardRequestPopup.isVisible = true
        binding.ivPopupBanner.isVisible = false
        binding.ivPopupAvatar.isVisible = true // Show profile for requests
        binding.llPopupContact.isVisible = false
        binding.llPopupDistance.isVisible = false
        binding.llPopupBloodType.isVisible = true
        
        binding.tvPopupName.text = request.requesterName
        binding.tvPopupRole.text = "Blood Recipient"
        binding.tvPopupBloodType.text = request.bloodType.label
        binding.tvPopupLocation.text = request.location
        
        val firstName = request.requesterName.split(" ").firstOrNull() ?: request.requesterName
        binding.btnPopupAction.text = "Message $firstName"

        // Zoom map to the pin
        if (request.latitude != null && request.longitude != null) {
            binding.webViewMap.evaluateJavascript(
                "javascript:map.flyTo([${request.latitude}, ${request.longitude}], 14, {animate: true});",
                null
            )
        }

        binding.btnPopupClose.setOnClickListener {
            binding.cardRequestPopup.isVisible = false
        }

        binding.btnPopupAction.setOnClickListener {
            binding.cardRequestPopup.isVisible = false
            val bundle = Bundle().apply { putString("requestId", request.requestId) }
            findNavController().navigate(R.id.action_home_to_request_detail, bundle)
        }
    }

    // ── State Observation ──────────────────────────────────────────────────────

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    bindData(state)

                    // Error
                    state.error?.let { err ->
                        binding.coordinatorHome.showErrorSnackbar(err)
                    }
                }
            }
        }
    }
    private fun bindData(state: HomeUiState) {
        
        // Update Map Legend (Critical, Urgent, Total Active, My Type)
        binding.tvCriticalCount.text = state.criticalCount.toString()
        binding.tvUrgentCount.text = state.urgentCount.toString()
        binding.tvTotalCount.text = state.totalCount.toString()
        binding.tvMyTypeCount.text = state.myTypeCount.toString()
        
        // Update Map Markers and List
        updateMapMarkers(state.filteredRequests, state.hospitals)
        adapter.submitList(state.filteredRequests)
    }

    private fun updateMapMarkers(
        requests: List<com.example.bloodbank.domain.model.BloodRequest>,
        hospitals: List<com.example.bloodbank.domain.model.HospitalMarker>
    ) {
        val jsonArray = JSONArray()
        requests.forEach { req ->
            if (req.latitude != null && req.longitude != null) {
                jsonArray.put(JSONObject().apply {
                    put("id", req.requestId)
                    put("lat", req.latitude)
                    put("lng", req.longitude)
                    put("type", "RECIPIENT")
                })
            }
        }
        hospitals.forEach { hosp ->
            jsonArray.put(JSONObject().apply {
                put("id", hosp.id)
                put("lat", hosp.latitude)
                put("lng", hosp.longitude)
                put("type", "HOSPITAL")
            })
        }
        val userLoc = viewModel.uiState.value.userLocation
        if (userLoc != null) {
            jsonArray.put(JSONObject().apply {
                put("id", "my_loc")
                put("lat", userLoc.first)
                put("lng", userLoc.second)
                put("type", "MY_LOCATION")
            })
        }
        // Use loadUrl so the raw JSON array is passed directly — no string-escaping issues
        binding.webViewMap.post {
            binding.webViewMap.loadUrl("javascript:updateMarkers($jsonArray)")
        }
    }
}
