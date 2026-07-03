package com.example.bloodbank.data.remote.dto

/**
 * Data Transfer Object for User.
 * Represents the exact schema stored in Firebase Firestore.
 */
data class UserDto(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val phoneNumber: String = "",
    val bloodType: String = "",
    val gender: String = "",
    val dateOfBirth: Long? = null,
    val role: String = "",
    val province: String = "",
    val city: String = "",
    val barangay: String = "",
    val street: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val profilePhotoUrl: String = "",
    val isVerified: Boolean = false,
    val hasCreatedRequest: Boolean = false,
    val createdAt: Long = 0L,
    
    // Donor specific
    val weightKg: Float? = null,
    val lastDonationDate: Long? = null,
    
    // Recipient specific
    val hospitalName: String = "",
    val hospitalAddress: String = ""
)
