package com.example.bloodbank.presentation.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.bloodbank.R
import com.example.bloodbank.databinding.FragmentHomeBinding
import com.example.bloodbank.domain.model.BloodRequest
import com.example.bloodbank.domain.model.HospitalMarker
import com.example.bloodbank.presentation.common.extensions.showErrorSnackbar
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * HomeFragment — Blood Request Feed (Map View Only)
 */
@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var adapter: BloodRequestAdapter

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
        setupPopupListeners()
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
            MaterialAlertDialogBuilder(requireContext())
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
                fetchCurrentLocation()
            } else {
                locationPermissionRequest.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
        }

        binding.fabFindHospital.setOnClickListener {
            binding.toggleView.check(R.id.btn_map)
            binding.cardRequestPopup.isVisible = false
            viewModel.setFilter(FeedFilter.HOSPITALS)
            
            viewModel.uiState.value.userLocation?.let { userLoc ->
                flyToLocation(userLoc.first, userLoc.second, 12)
            } ?: run {
                flyToLocation(12.8797, 121.7740, 6)
            }
            
            Snackbar.make(binding.root, "🏥 Showing hospitals & blood centers near you", Snackbar.LENGTH_SHORT)
                .setBackgroundTint(android.graphics.Color.parseColor("#1976D2"))
                .setTextColor(android.graphics.Color.WHITE)
                .show()
        }
    }

    // ── List View & Filters ───────────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = BloodRequestAdapter(
            onRespondClick = { navigateToDetail(it.requestId) }
        )
        binding.rvListView.adapter = adapter
        binding.rvListView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun navigateToDetail(requestId: String) {
        val bundle = Bundle().apply { putString("requestId", requestId) }
        findNavController().navigate(R.id.action_home_to_request_detail, bundle)
    }

    private fun setupToggleAndFilters() {
        binding.toggleView.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val isMap = checkedId == R.id.btn_map
                binding.webViewMap.isVisible = isMap
                binding.cardLegend.isVisible = isMap
                binding.rvListView.isVisible = !isMap
                
                if (!isMap) binding.cardRequestPopup.isVisible = false
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocation() {
        Snackbar.make(binding.root, "📍 Fetching location...", Snackbar.LENGTH_SHORT).show()
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    viewModel.updateUserLocation(location.latitude, location.longitude)
                    binding.toggleView.check(R.id.btn_map)
                    flyToLocation(location.latitude, location.longitude, 14)
                    Snackbar.make(binding.root, "📍 Location saved & updated", Snackbar.LENGTH_SHORT).show()
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
                    if (_binding == null) return
                    val state = viewModel.uiState.value
                    updateMapMarkers(state.filteredRequests, state.hospitals)
                }
            }
            loadUrl("file:///android_asset/map.html")
        }
    }

    private fun flyToLocation(lat: Double, lng: Double, zoom: Int = 14) {
        binding.webViewMap.evaluateJavascript(
            "javascript:map.flyTo([$lat, $lng], $zoom, {animate: true, duration: 1});", null
        )
    }

    inner class MapJavascriptInterface {
        @JavascriptInterface
        fun onMarkerClick(id: String) {
            requireActivity().runOnUiThread {
                if (id == "my_loc") {
                    showMyLocationPopup()
                } else if (id.startsWith("h")) {
                    viewModel.uiState.value.hospitals.find { it.id == id }?.let { showHospitalPopup(it) }
                } else {
                    viewModel.uiState.value.filteredRequests.find { it.requestId == id }?.let { showRequestPopup(it) }
                }
            }
        }
    }

    private fun setupPopupListeners() {
        binding.btnPopupClose.setOnClickListener {
            binding.cardRequestPopup.isVisible = false
        }
    }

    private fun resetPopupState() {
        binding.apply {
            cardRequestPopup.isVisible = true
            ivPopupBanner.isVisible = false
            ivPopupAvatar.isVisible = false
            llPopupContact.isVisible = false
            llPopupDistance.isVisible = false
            llPopupBloodType.isVisible = false
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showMyLocationPopup() {
        resetPopupState()
        binding.apply {
            tvPopupName.text = "You are here"
            tvPopupRole.text = "Current Location"
            tvPopupBloodType.text = "📍"
            tvPopupLocation.text = "Your detected GPS location."
            btnPopupAction.text = "Search Nearby Hospitals"
            
            btnPopupAction.setOnClickListener {
                cardRequestPopup.isVisible = false
                fabFindHospital.performClick()
            }
        }
        viewModel.uiState.value.userLocation?.let {
            flyToLocation(it.first, it.second, 14)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showHospitalPopup(hospital: HospitalMarker) {
        resetPopupState()
        binding.apply {
            tvPopupName.text = hospital.name
            tvPopupRole.text = "🏥 Hospital / Blood Center"
            tvPopupLocation.text = hospital.address
            
            if (!hospital.imageUrl.isNullOrEmpty()) {
                ivPopupBanner.isVisible = true
                Glide.with(requireContext())
                    .load(hospital.imageUrl)
                    .placeholder(R.drawable.ic_menu_hospital)
                    .error(R.drawable.ic_menu_hospital)
                    .centerCrop()
                    .into(ivPopupBanner)
            }
            
            if (!hospital.contactNumber.isNullOrEmpty()) {
                llPopupContact.isVisible = true
                tvPopupContact.text = hospital.contactNumber
            }
            
            viewModel.uiState.value.userLocation?.let { userLoc ->
                val distKm = calculateDistance(userLoc.first, userLoc.second, hospital.latitude, hospital.longitude)
                llPopupDistance.isVisible = true
                tvPopupDistance.text = String.format("%.1f km away", distKm)
            }

            btnPopupAction.text = "Get Directions"
            btnPopupAction.setOnClickListener {
                val gmmIntentUri = android.net.Uri.parse("geo:0,0?q=${android.net.Uri.encode(hospital.name)}")
                val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
                    startActivity(mapIntent)
                } else {
                    coordinatorHome.showErrorSnackbar("Google Maps app not found")
                }
                cardRequestPopup.isVisible = false
            }
        }
        flyToLocation(hospital.latitude, hospital.longitude, 14)
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth's radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c // Distance in km
    }

    @SuppressLint("SetTextI18n")
    private fun showRequestPopup(request: BloodRequest) {
        resetPopupState()
        binding.apply {
            ivPopupAvatar.isVisible = true
            llPopupBloodType.isVisible = true
            
            tvPopupName.text = request.requesterName
            tvPopupRole.text = "Blood Recipient"
            tvPopupBloodType.text = request.bloodType.label
            tvPopupLocation.text = request.location
            
            val firstName = request.requesterName.split(" ").firstOrNull() ?: request.requesterName
            btnPopupAction.text = "Message $firstName"

            btnPopupAction.setOnClickListener {
                cardRequestPopup.isVisible = false
                navigateToDetail(request.requestId)
            }
        }
        if (request.latitude != null && request.longitude != null) {
            flyToLocation(request.latitude, request.longitude, 14)
        }
    }

    // ── State Observation ──────────────────────────────────────────────────────

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    bindData(state)
                    state.error?.let { err ->
                        binding.coordinatorHome.showErrorSnackbar(err)
                    }
                }
            }
        }
    }
    
    private fun bindData(state: HomeUiState) {
        binding.tvCriticalCount.text = state.criticalCount.toString()
        binding.tvUrgentCount.text = state.urgentCount.toString()
        binding.tvTotalCount.text = state.totalCount.toString()
        binding.tvMyTypeCount.text = state.myTypeCount.toString()
        
        updateMapMarkers(state.filteredRequests, state.hospitals)
        adapter.submitList(state.filteredRequests)
    }

    private fun updateMapMarkers(
        requests: List<BloodRequest>,
        hospitals: List<HospitalMarker>
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
        viewModel.uiState.value.userLocation?.let { userLoc ->
            jsonArray.put(JSONObject().apply {
                put("id", "my_loc")
                put("lat", userLoc.first)
                put("lng", userLoc.second)
                put("type", "MY_LOCATION")
            })
        }
        
        if (_binding == null) return
        
        binding.webViewMap.post {
            if (_binding != null) {
                binding.webViewMap.loadUrl("javascript:updateMarkers($jsonArray)")
            }
        }
    }
}
