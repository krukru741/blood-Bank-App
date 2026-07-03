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
            // Zoom out to center of Philippines for now
            binding.webViewMap.evaluateJavascript(
                "javascript:map.flyTo([12.8797, 121.7740], 6, {animate: true});",
                null
            )
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

    // ── Map Setup ──────────────────────────────────────────────────────────────
    
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webViewMap.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
            addJavascriptInterface(MapJavascriptInterface(), "Android")
            loadUrl("file:///android_asset/map.html")
        }
    }

    inner class MapJavascriptInterface {
        @JavascriptInterface
        fun onMarkerClick(id: String) {
            requireActivity().runOnUiThread {
                if (id.startsWith("h")) {
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
    private fun showHospitalPopup(hospital: com.example.bloodbank.domain.model.HospitalMarker) {
        binding.cardRequestPopup.isVisible = true
        binding.tvPopupName.text = hospital.name
        binding.tvPopupRole.text = "🏥 Hospital / Blood Center"
        binding.tvPopupBloodType.text = "🩸"
        binding.tvPopupLocation.text = "${hospital.address}\n📍 Approx. location — tap for directions"
        
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
                startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, gmmIntentUri))
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showRequestPopup(request: com.example.bloodbank.domain.model.BloodRequest) {
        binding.cardRequestPopup.isVisible = true
        binding.tvPopupName.text = request.requesterName
        binding.tvPopupRole.text = "Blood Recipient"
        binding.tvPopupBloodType.text = request.bloodType.label
        binding.tvPopupLocation.text = request.hospital.ifEmpty { request.location }
        
        // Extract first name for the button
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

    private fun updateMapMarkers(requests: List<com.example.bloodbank.domain.model.BloodRequest>, hospitals: List<com.example.bloodbank.domain.model.HospitalMarker>) {
        val jsonArray = JSONArray()
        requests.forEach { req ->
            if (req.latitude != null && req.longitude != null) {
                val obj = JSONObject().apply {
                    put("id", req.requestId)
                    put("lat", req.latitude)
                    put("lng", req.longitude)
                    put("type", "RECIPIENT")
                }
                jsonArray.put(obj)
            }
        }
        hospitals.forEach { hosp ->
            val obj = JSONObject().apply {
                put("id", hosp.id)
                put("lat", hosp.latitude)
                put("lng", hosp.longitude)
                put("type", "HOSPITAL")
            }
            jsonArray.put(obj)
        }
        val jsonStr = jsonArray.toString().replace("'", "\\'")
        binding.webViewMap.evaluateJavascript("javascript:updateMarkers('$jsonStr')", null)
    }
}
