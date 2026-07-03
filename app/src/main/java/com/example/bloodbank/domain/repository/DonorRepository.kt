package com.example.bloodbank.domain.repository

import com.example.bloodbank.domain.model.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Donor — lightweight domain model for donor-specific data.
 * (Full profile lives in [com.example.bloodbank.domain.model.User])
 */
data class DonorProfile(
    val uid: String = "",
    val totalDonations: Int = 0,
    val lastDonationDate: Long? = null,
    val nextEligibleDate: Long? = null,   // 56 days (8 weeks) after last donation
    val isDonationEligible: Boolean = true,
    val badges: List<String> = emptyList() // gamification badges
)

/**
 * DonorRepository — manages donor-specific data (separate from user profile).
 */
interface DonorRepository {

    /** Fetches donor-specific stats (donation count, eligibility, badges). */
    fun getDonorProfile(uid: String): Flow<Resource<DonorProfile>>

    /** Records a completed donation and updates eligibility dates. */
    fun recordDonation(uid: String, donationTimestamp: Long): Flow<Resource<Unit>>

    /** Real-time stream of all eligible donors matching a given blood type. */
    fun observeEligibleDonors(bloodTypeLabel: String): Flow<Resource<List<DonorProfile>>>

    /** Updates gamification badges for a donor. */
    fun updateBadges(uid: String, badges: List<String>): Flow<Resource<Unit>>
}
