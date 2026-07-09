package com.example.bloodbank.presentation.request

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bloodbank.R
import com.example.bloodbank.databinding.ItemMyRequestBinding
import com.example.bloodbank.domain.model.BloodRequest
import com.example.bloodbank.domain.model.RequestStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyRequestsAdapter(
    private val onViewDetailsClick: (BloodRequest) -> Unit
) : ListAdapter<BloodRequest, MyRequestsAdapter.ViewHolder>(DIFF_CALLBACK) {

    inner class ViewHolder(
        private val binding: ItemMyRequestBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(request: BloodRequest) {
            with(binding) {
                tvPatientName.text = request.requesterName.ifBlank { "Unknown Patient" }
                tvBloodTypeBadge.text = request.bloodType.label
                tvHospital.text = request.hospital
                tvQuantity.text = "${request.unitsNeeded} unit${if (request.unitsNeeded > 1) "s" else ""}"
                
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                tvDate.text = "Posted: ${sdf.format(Date(request.createdAt))}"

                val statusText = when (request.status) {
                    RequestStatus.PENDING -> "Pending"
                    RequestStatus.MATCHED -> "Matched"
                    RequestStatus.FULFILLED -> "Fulfilled"
                    RequestStatus.CANCELLED -> "Cancelled"
                    RequestStatus.EXPIRED -> "Expired"
                }
                
                val statusColor = when (request.status) {
                    RequestStatus.PENDING -> R.color.warning_amber
                    RequestStatus.MATCHED -> R.color.success_green
                    RequestStatus.FULFILLED -> R.color.gray_text
                    RequestStatus.CANCELLED -> R.color.error_red
                    RequestStatus.EXPIRED -> R.color.gray_text
                }
                
                chipStatus.text = statusText
                chipStatus.setTextColor(itemView.context.getColor(statusColor))

                btnAction.setOnClickListener { onViewDetailsClick(request) }
                root.setOnClickListener { onViewDetailsClick(request) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMyRequestBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<BloodRequest>() {
            override fun areItemsTheSame(old: BloodRequest, new: BloodRequest) =
                old.requestId == new.requestId

            override fun areContentsTheSame(old: BloodRequest, new: BloodRequest) =
                old == new
        }
    }
}
