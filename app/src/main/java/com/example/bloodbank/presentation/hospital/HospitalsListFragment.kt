package com.example.bloodbank.presentation.hospital

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bloodbank.R
import com.example.bloodbank.databinding.FragmentHospitalsListBinding
import com.example.bloodbank.presentation.common.extensions.showErrorSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HospitalsListFragment : Fragment() {

    private var _binding: FragmentHospitalsListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HospitalsListViewModel by viewModels()
    private val hospitalsAdapter = HospitalsAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHospitalsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupSearchView()
        setupFab()
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

    private fun setupRecyclerView() {
        binding.rvHospitals.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = hospitalsAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.search(query ?: "")
                binding.searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.search(newText ?: "")
                return true
            }
        })
    }

    private fun setupFab() {
        binding.fabAddHospital.setOnClickListener {
            findNavController().navigate(R.id.action_hospitalsListFragment_to_addHospitalFragment)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is HospitalsListUiState.Loading -> {
                            binding.progressBar.isVisible = true
                            binding.rvHospitals.isVisible = false
                            binding.layoutEmptyState.isVisible = false
                        }
                        is HospitalsListUiState.Success -> {
                            binding.progressBar.isVisible = false
                            hospitalsAdapter.submitList(state.hospitals)
                            
                            binding.rvHospitals.isVisible = state.hospitals.isNotEmpty()
                            binding.layoutEmptyState.isVisible = state.hospitals.isEmpty()
                        }
                        is HospitalsListUiState.Error -> {
                            binding.progressBar.isVisible = false
                            binding.root.showErrorSnackbar(state.message)
                        }
                    }
                }
            }
        }
    }
}
