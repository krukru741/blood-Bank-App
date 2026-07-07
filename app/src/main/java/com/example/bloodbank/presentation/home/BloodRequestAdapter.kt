package com.example.bloodbank.presentation.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bloodbank.R
import com.example.bloodbank.databinding.ItemBloodRequestBinding
import com.example.bloodbank.domain.model.BloodRequest
import com.example.bloodbank.domain.model.UrgencyLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * BloodRequestAdapter
 *
 * Modern RecyclerView adapter using [ListAdapter] + [DiffUtil.ItemCallback].
 * ListAdapter handles diff calculations on a background thread automatically.
 *
 * Key features:
 * - Urgency color strip (left border background changes per urgency level)
 * - Blood type badge (circular red badge)
 * - "Time ago" formatting (2h ago, 3d ago, etc.)
 * - Description shown only when non-empty
 * - Respond/View button callback
 */
class BloodRequestAdapter(
    private val onRespondClick: (BloodRequest) -> Unit
) : ListAdapter<BloodRequest, BloodRequestAdapter.ViewHolder>(DIFF_CALLBACK) {

    inner class ViewHolder(
        private val binding: ItemBloodRequestBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(request: BloodRequest) {
            with(binding) {
                // ── Urgency color strip ────────────────────────────────────
                urgencyStrip.setBackgroundResource(
                    when (request.urgency) {
                        UrgencyLevel.CRITICAL -> R.drawable.bg_urgency_critical
                        UrgencyLevel.URGENT   -> R.drawable.bg_urgency_urgent
                        UrgencyLevel.NORMAL   -> R.drawable.bg_urgency_normal
                    }
                )

                // ── Blood type badge ───────────────────────────────────────
                tvBloodType.text = request.bloodType.label

                // ── Requester info ─────────────────────────────────────────
                tvRequesterName.text = request.requesterName.ifBlank { "Anonymous" }
                tvHospital.text = "📍 ${request.hospital} • ${request.location}"

                // ── Time posted ────────────────────────────────────────────
                tvTimeAgo.text = request.createdAt.toTimeAgo()

                // ── Description (only if non-empty) ───────────────────────
                val hasDesc = request.description.isNotBlank()
                tvDescription.isVisible = hasDesc
                if (hasDesc) tvDescription.text = request.description

                // ── Units needed ───────────────────────────────────────────
                tvUnits.text = "${request.unitsNeeded} unit${if (request.unitsNeeded > 1) "s" else ""}"

                // ── Urgency label ──────────────────────────────────────────
                val (labelText, labelColor) = when (request.urgency) {
                    UrgencyLevel.CRITICAL -> "⚠ CRITICAL" to R.color.urgency_critical
                    UrgencyLevel.URGENT   -> "⏰ URGENT"   to R.color.urgency_urgent
                    UrgencyLevel.NORMAL   -> "✓ NORMAL"   to R.color.urgency_normal
                }
                tvUrgencyLabel.text = labelText
                tvUrgencyLabel.setTextColor(
                    itemView.context.getColor(labelColor)
                )

                // ── Click listeners ────────────────────────────────────────
                btnRespond.setOnClickListener { onRespondClick(request) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBloodRequestBinding.inflate(
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
                old == new   // data class equality checks all fields
        }
    }
}

// ── Time formatting helper ─────────────────────────────────────────────────────
private fun Long.toTimeAgo(): String {
    val now  = System.currentTimeMillis()
    val diff = now - this
    return when {
        diff < TimeUnit.MINUTES.toMillis(1)  -> "Just now"
        diff < TimeUnit.HOURS.toMillis(1)    -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
        diff < TimeUnit.DAYS.toMillis(1)     -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
        diff < TimeUnit.DAYS.toMillis(7)     -> "${TimeUnit.MILLISECONDS.toDays(diff)}d ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(this))
    }
}
