package com.example.bloodbank.data.repository

import com.example.bloodbank.domain.model.Resource
import com.example.bloodbank.domain.repository.DonorProfile
import com.example.bloodbank.domain.repository.DonorRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/** TODO (Step 3): Implement real Firestore-backed donor profile logic. */
@Singleton
class DonorRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : DonorRepository {

    companion object {
        private const val DONORS_COLLECTION = "donors"
        private const val DONATION_INTERVAL_MS = 56L * 24 * 60 * 60 * 1000 // 56 days
    }

    override fun getDonorProfile(uid: String): Flow<Resource<DonorProfile>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(DonorProfile(uid = uid))) // stub
    }

    override fun recordDonation(uid: String, donationTimestamp: Long): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        // TODO: Update Firestore donor document — increment count, set lastDonationDate,
        //       calculate nextEligibleDate = donationTimestamp + DONATION_INTERVAL_MS
        emit(Resource.Success(Unit))
    }

    override fun observeEligibleDonors(bloodTypeLabel: String): Flow<Resource<List<DonorProfile>>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(emptyList()))
    }

    override fun updateBadges(uid: String, badges: List<String>): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(Unit))
    }
}
