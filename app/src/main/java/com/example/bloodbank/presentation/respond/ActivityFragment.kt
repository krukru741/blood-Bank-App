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
import com.example.bloodbank.R
import com.example.bloodbank.databinding.FragmentActivityBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ActivityFragment : Fragment() {

    private var _binding: FragmentActivityBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ActivityViewModel by viewModels()

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
                    
                    if (!state.isLoading) {
                        if (!state.isDonor) {
                            binding.layoutNotDonor.isVisible = true
                            binding.layoutDonations.isVisible = false
                        } else {
                            binding.layoutNotDonor.isVisible = false
                            binding.layoutDonations.isVisible = true
                            
                            // Check if empty
                            binding.layoutNoDonations.isVisible = state.donations.isEmpty()
                            binding.rvDonations.isVisible = state.donations.isNotEmpty()
                            
                            // TODO: Set up RecyclerView adapter
                        }
                    } else {
                        binding.layoutNotDonor.isVisible = false
                        binding.layoutDonations.isVisible = false
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
