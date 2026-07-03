package com.example.bloodbank.presentation.request

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
import com.example.bloodbank.databinding.FragmentMyRequestsBinding
import com.example.bloodbank.domain.model.Resource
import com.example.bloodbank.domain.repository.AuthRepository
import com.example.bloodbank.domain.repository.BloodRequestRepository
import com.example.bloodbank.presentation.home.BloodRequestAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MyRequestsFragment
 *
 * Shows blood requests created by the currently logged-in user.
 * Reuses [BloodRequestAdapter] for the list.
 */
@AndroidEntryPoint
class MyRequestsFragment : Fragment() {

    private var _binding: FragmentMyRequestsBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var bloodRequestRepository: BloodRequestRepository
    @Inject lateinit var authRepository: AuthRepository

    private lateinit var adapter: BloodRequestAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = BloodRequestAdapter(
            onRespondClick = { /* donors respond — recipient's own list, no action */ },
            onCardClick    = { request ->
                val bundle = Bundle().apply {
                    putString("requestId", request.requestId)
                }
                findNavController().navigate(R.id.action_my_requests_to_request_detail, bundle)
            }
        )

        binding.rvMyRequests.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter       = this@MyRequestsFragment.adapter
        }

        binding.btnCreateFirstRequest.setOnClickListener {
            findNavController().navigate(R.id.action_my_requests_to_create)
        }

        observeMyRequests()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeMyRequests() {
        val uid = authRepository.currentUser?.uid ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                bloodRequestRepository.observeMyRequests(uid).collect { resource ->
                    when (resource) {
                        is Resource.Loading -> binding.progressMyRequests.isVisible = true
                        is Resource.Success -> {
                            binding.progressMyRequests.isVisible = false
                            val list = resource.data
                            adapter.submitList(list)
                            binding.emptyMyRequests.isVisible = list.isEmpty()
                            binding.rvMyRequests.isVisible    = list.isNotEmpty()
                        }
                        is Resource.Error -> {
                            binding.progressMyRequests.isVisible = false
                        }
                    }
                }
            }
        }
    }
}
