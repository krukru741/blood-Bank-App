package com.example.bloodbank.presentation.common.validation

import java.util.Calendar

/**
 * FormValidator — client-side validation for all registration fields.
 * Returns null on success, or a human-readable error String on failure.
 *
 * Medical eligibility rules (WHO / Philippine Red Cross standards):
 * - Age: 18–65 years
 * - Weight: ≥ 50 kg
 * - Donation interval: ≥ 56 days (whole blood)
 */
object FormValidator {

    // ── Common fields ─────────────────────────────────────────────────────────

    fun validateEmail(email: String): String? {
        if (email.isBlank()) return "Email is required"
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
            return "Enter a valid email address"
        return null
    }

    fun validatePassword(password: String): String? {
        if (password.isBlank()) return "Password is required"
        if (password.length < 8) return "Password must be at least 8 characters"
        if (!password.any { it.isDigit() }) return "Include at least one number"
        if (!password.any { it.isLetter() }) return "Include at least one letter"
        return null
    }

    fun validateConfirmPassword(password: String, confirm: String): String? {
        if (confirm.isBlank()) return "Please confirm your password"
        if (password != confirm) return "Passwords do not match"
        return null
    }

    fun validateFullName(name: String): String? {
        if (name.isBlank()) return "Full name is required"
        if (name.trim().length < 2) return "Name must be at least 2 characters"
        return null
    }

    fun validatePhoneNumber(phone: String): String? {
        if (phone.isBlank()) return "Phone number is required"
        val digits = phone.filter { it.isDigit() }
        if (digits.length < 10) return "Enter a valid phone number (min 10 digits)"
        return null
    }

    fun validateProvince(province: String): String? {
        if (province.isBlank()) return "Province is required"
        return null
    }

    fun validateCity(city: String): String? {
        if (city.isBlank()) return "City/Municipality is required"
        return null
    }

    fun validateBarangay(barangay: String): String? {
        if (barangay.isBlank()) return "Barangay is required"
        return null
    }

    fun validateStreet(street: String): String? {
        if (street.isBlank()) return "Street/Block/Lot is required for accurate location"
        return null
    }

    // ── Donor-specific fields ─────────────────────────────────────────────────

    /**
     * Validates the donor's date of birth.
     * Requirements: age must be between 18 and 65 years.
     *
     * @param dobEpochMs Date of birth in epoch milliseconds (null = not provided)
     */
    fun validateDateOfBirth(dobEpochMs: Long?): String? {
        if (dobEpochMs == null) return "Date of birth is required"

        val today = Calendar.getInstance()
        val dob   = Calendar.getInstance().apply { timeInMillis = dobEpochMs }

        var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) age--

        return when {
            age < 18 -> "Donors must be at least 18 years old"
            age > 65 -> "Donors must be 65 years old or younger"
            else     -> null
        }
    }

    /**
     * Validates the donor's body weight.
     * Requirements: ≥ 50 kg (110 lbs) — WHO / Philippine Red Cross standard.
     *
     * @param weightText Raw text from the weight input field
     */
    fun validateWeight(weightText: String): String? {
        if (weightText.isBlank()) return "Weight is required for donors"
        val weight = weightText.toFloatOrNull()
            ?: return "Enter a valid weight (e.g., 65.5)"
        if (weight < 50f) return "Donors must weigh at least 50 kg"
        if (weight > 250f) return "Please enter a realistic weight"
        return null
    }

    /**
     * Validates the last donation date (optional field).
     * If provided, next eligible date = lastDonation + 56 days.
     *
     * @param lastDonationEpochMs Last donation in epoch ms (null = first-time donor — skip check)
     */
    fun validateLastDonationDate(lastDonationEpochMs: Long?): String? {
        if (lastDonationEpochMs == null) return null // First-time donor — OK

        val minInterval = 56L * 24 * 60 * 60 * 1000 // 56 days in ms
        val nextEligible = lastDonationEpochMs + minInterval

        return if (System.currentTimeMillis() < nextEligible) {
            val daysLeft = ((nextEligible - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt()
            "You must wait $daysLeft more day(s) before donating again"
        } else null
    }

    // ── Recipient-specific fields ─────────────────────────────────────────────

    fun validateHospitalName(name: String): String? {
        if (name.isBlank()) return "Hospital name is required for recipients"
        if (name.trim().length < 3) return "Enter the full hospital name"
        return null
    }

    fun validateHospitalAddress(address: String): String? {
        if (address.isBlank()) return "Hospital address/location is required"
        return null
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /** Returns true only if ALL passed validators return null (no errors). */
    fun allValid(vararg results: String?): Boolean = results.all { it == null }
}
