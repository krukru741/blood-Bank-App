package com.example.bloodbank.presentation.respond

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bloodbank.R
import com.example.bloodbank.databinding.FragmentActivityBinding
import com.example.bloodbank.presentation.home.BloodRequestAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ActivityFragment : Fragment() {

    private var _binding: FragmentActivityBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ActivityViewModel by viewModels()
    private lateinit var adapter: BloodRequestAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        adapter = BloodRequestAdapter(
            onRespondClick = { request ->
                val bundle = Bundle().apply {
                    putString("requestId", request.requestId)
                }
                findNavController().navigate(R.id.action_activity_to_request_detail, bundle)
            }
        )
        binding.rvDonations.adapter = adapter
        binding.rvDonations.layoutManager = LinearLayoutManager(requireContext())
        
        setupListeners()
        observeUiState()
    }

    private fun setupListeners() {
        binding.btnBecomeDonor.setOnClickListener {
            findNavController().navigate(R.id.action_activity_to_become_donor)
        }
        binding.btnFindRequests.setOnClickListener {
            // Navigate back to Home
            findNavController().navigate(R.id.homeFragment)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressActivity.isVisible = state.isLoading
                    
                    val isDonorAndLoaded = !state.isLoading && state.isDonor
                    binding.layoutNotDonor.isVisible = !state.isLoading && !state.isDonor
                    binding.layoutDonations.isVisible = isDonorAndLoaded
                    
                    binding.layoutNoDonations.isVisible = isDonorAndLoaded && state.donations.isEmpty()
                    binding.rvDonations.isVisible = isDonorAndLoaded && state.donations.isNotEmpty()
                    
                    if (isDonorAndLoaded) {
                        adapter.submitList(state.donations)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
