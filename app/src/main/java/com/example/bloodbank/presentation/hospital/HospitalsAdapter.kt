package com.example.bloodbank.presentation.hospital

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bloodbank.databinding.ItemHospitalCardBinding
import com.example.bloodbank.domain.model.HospitalMarker

class HospitalsAdapter : ListAdapter<HospitalMarker, HospitalsAdapter.HospitalViewHolder>(DiffCallback) {

    class HospitalViewHolder(
        private val binding: ItemHospitalCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(hospital: HospitalMarker) {
            binding.tvHospitalName.text = hospital.name
            
            // Format address: Street, City
            val fullAddress = buildString {
                if (hospital.address.isNotBlank()) append(hospital.address)
                if (hospital.address.isNotBlank() && !hospital.city.isNullOrBlank()) append(", ")
                if (!hospital.city.isNullOrBlank()) append(hospital.city)
            }
            binding.tvAddress.text = "📍 $fullAddress"
            
            // Type
            if (!hospital.type.isNullOrBlank()) {
                binding.tvHospitalType.isVisible = true
                binding.tvHospitalType.text = hospital.type
            } else {
                binding.tvHospitalType.isVisible = false
            }
            
            // Contact
            if (!hospital.contactNumber.isNullOrBlank()) {
                binding.tvContact.isVisible = true
                binding.tvContact.text = "📞 ${hospital.contactNumber}"
            } else {
                binding.tvContact.isVisible = false
            }
            
            // Emergency Contact
            if (!hospital.emergencyContact.isNullOrBlank()) {
                binding.tvEmergencyContact.isVisible = true
                binding.tvEmergencyContact.text = "🚑 ${hospital.emergencyContact}"
            } else {
                binding.tvEmergencyContact.isVisible = false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HospitalViewHolder {
        val binding = ItemHospitalCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HospitalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HospitalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<HospitalMarker>() {
            override fun areItemsTheSame(oldItem: HospitalMarker, newItem: HospitalMarker): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: HospitalMarker, newItem: HospitalMarker): Boolean {
                return oldItem == newItem
            }
        }
    }
}
