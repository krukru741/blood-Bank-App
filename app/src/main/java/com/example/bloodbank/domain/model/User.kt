package com.example.bloodbank.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// ── BloodType ─────────────────────────────────────────────────────────────────
enum class BloodType(val label: String) {
    A_POSITIVE("A+"),
    A_NEGATIVE("A-"),
    B_POSITIVE("B+"),
    B_NEGATIVE("B-"),
    AB_POSITIVE("AB+"),
    AB_NEGATIVE("AB-"),
    O_POSITIVE("O+"),
    O_NEGATIVE("O-");

    override fun toString(): String = label

    companion object {
        fun fromLabel(label: String): BloodType =
            entries.firstOrNull { it.label == label } ?: O_POSITIVE
    }
}

// ── Gender ────────────────────────────────────────────────────────────────────
/**
 * Gender is medically relevant for blood donation:
 * - Hemoglobin thresholds differ (Male ≥13 g/dL, Female ≥12.5 g/dL)
 * - Males can donate every 12 weeks; females every 16 weeks (whole blood)
 */
enum class Gender(val label: String) {
    MALE("Male"),
    FEMALE("Female"),
    NOT_SPECIFIED("Prefer not to say");

    companion object {
        fun fromLabel(label: String): Gender =
            entries.firstOrNull { it.label == label } ?: NOT_SPECIFIED
    }
}

// ── UserRole ──────────────────────────────────────────────────────────────────
enum class UserRole { USER, DONOR, RECIPIENT, ADMIN }

// ── User (central domain model) ───────────────────────────────────────────────
/**
 * User — central domain entity. Fields are role-dependent:
 *
 * ### Common (all roles)
 * @param uid             Firebase UID
 * @param email           Registered email
 * @param displayName     Full name
 * @param phoneNumber     Contact number
 * @param bloodType       ABO+Rh blood type
 * @param gender          Biological sex — affects eligibility checks
 * @param dateOfBirth     Epoch ms — donors must be 18–65 years old
 * @param role            DONOR, RECIPIENT, or ADMIN
 * @param city            City/municipality for proximity matching
 * @param profilePhotoUrl Firebase Storage URL for profile photo
 * @param isVerified      True once email is verified
 * @param createdAt       Account creation timestamp (epoch ms)
 *
 * ### Donor-only
 * @param weightKg        Body weight — must be ≥ 50 kg to donate
 * @param lastDonationDate Epoch ms — next eligibility = +56 days (whole blood)
 *
 * ### Recipient-only
 * @param hospitalName    Hospital where patient is admitted
 * @param hospitalAddress Full hospital address for location display
 */
@Parcelize
data class User(
    // ── Common ────────────────────────────────────────────────────────────────
    val uid: String             = "",
    val email: String           = "",
    val displayName: String     = "",
    val phoneNumber: String     = "",
    val bloodType: BloodType    = BloodType.O_POSITIVE,
    val gender: Gender          = Gender.NOT_SPECIFIED,
    val dateOfBirth: Long?      = null,   // epoch ms; null = not provided
    val role: UserRole          = UserRole.USER,
    val province: String        = "",
    val city: String            = "",
    val barangay: String        = "",
    val street: String          = "",
    val latitude: Double?       = null,
    val longitude: Double?      = null,
    val profilePhotoUrl: String = "",
    val isVerified: Boolean     = false,
    val hasCreatedRequest: Boolean = false,
    val createdAt: Long         = System.currentTimeMillis(),

    // ── Donor-specific ────────────────────────────────────────────────────────
    val weightKg: Float?        = null,   // must be ≥ 50 kg
    val lastDonationDate: Long? = null,   // epoch ms; null = first-time donor
    val isAvailableToDonate: Boolean = true, // Toggle for donor availability

    // ── Recipient-specific ────────────────────────────────────────────────────
    val hospitalName: String    = "",
    val hospitalAddress: String = ""
) : Parcelable {

    /** True if this donor is currently eligible to donate (based on 56-day rule). */
    val isDonationEligible: Boolean
        get() {
            val last = lastDonationDate ?: return true
            val minInterval = 56L * 24 * 60 * 60 * 1000 // 56 days in ms
            return System.currentTimeMillis() - last >= minInterval
        }

    /** Age in years, derived from dateOfBirth. Returns null if DOB not provided. */
    val ageYears: Int?
        get() {
            val dob = dateOfBirth ?: return null
            val now = java.util.Calendar.getInstance()
            val birth = java.util.Calendar.getInstance().apply { timeInMillis = dob }
            var age = now.get(java.util.Calendar.YEAR) - birth.get(java.util.Calendar.YEAR)
            if (now.get(java.util.Calendar.DAY_OF_YEAR) < birth.get(java.util.Calendar.DAY_OF_YEAR)) age--
            return age
        }
}
