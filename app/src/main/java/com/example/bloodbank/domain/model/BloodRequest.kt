package com.example.bloodbank.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * BloodRequest — a request for blood from a recipient or hospital.
 *
 * @param requestId     Unique Firestore document ID
 * @param requesterId   UID of the user who created this request
 * @param requesterName Display name of the requester
 * @param bloodType     Blood type needed
 * @param unitsNeeded   How many units (pints) are required
 * @param hospital      Hospital name and address
 * @param location      City/municipality for proximity matching
 * @param urgency       Priority level of this request
 * @param status        Current fulfilment status
 * @param description   Additional medical context
 * @param contactNumber Direct contact for coordination
 * @param createdAt     Request creation timestamp (epoch ms)
 * @param expiresAt     After this timestamp, request is auto-expired
 */
@Parcelize
data class BloodRequest(
    val requestId: String = "",
    val requesterId: String = "",
    val requesterName: String = "",
    val bloodType: BloodType = BloodType.O_POSITIVE,
    val unitsNeeded: Int = 1,
    val hospital: String = "",
    val location: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val urgency: UrgencyLevel = UrgencyLevel.NORMAL,
    val status: RequestStatus = RequestStatus.PENDING,
    val description: String = "",
    val contactNumber: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = createdAt + (7 * 24 * 60 * 60 * 1000L), // 7 days default
    val acceptedByDonorId: String? = null,
    val acceptedByDonorName: String? = null,
    val acceptedByDonorPhone: String? = null
) : Parcelable

/** Priority level of a blood request. */
enum class UrgencyLevel {
    CRITICAL,   // Life-threatening — needs within hours
    URGENT,     // Needs within 24 hours
    NORMAL      // Scheduled surgery / elective
}

/** Lifecycle status of a blood request. */
enum class RequestStatus {
    PENDING,    // Awaiting donor response
    MATCHED,    // A donor has been identified
    FULFILLED,  // Blood has been donated
    EXPIRED,    // Past expiry date, not fulfilled
    CANCELLED   // Manually cancelled by requester
}
