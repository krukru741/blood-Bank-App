package com.example.bloodbank.data.remote.dto

/**
 * Data Transfer Object for BloodRequest.
 * Represents the exact schema stored in Firebase Firestore.
 */
data class BloodRequestDto(
    val requestId: String = "",
    val requesterId: String = "",
    val requesterName: String = "",
    val bloodType: String = "",
    val unitsNeeded: Int = 1,
    val hospital: String = "",
    val location: String = "",
    val urgency: String = "",
    val status: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val description: String = "",
    val contactNumber: String = "",
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,
    val acceptedByDonorId: String? = null,
    val acceptedByDonorName: String? = null,
    val acceptedByDonorPhone: String? = null
)
