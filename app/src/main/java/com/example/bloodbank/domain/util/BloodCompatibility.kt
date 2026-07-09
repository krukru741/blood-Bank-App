package com.example.bloodbank.domain.util

import com.example.bloodbank.domain.model.BloodType

object BloodCompatibility {
    
    /**
     * Checks if a donor's blood type can be safely transfused to a patient's blood type.
     * 
     * Medical Rules:
     * - O- can donate to everyone.
     * - O+ can donate to O+, A+, B+, AB+.
     * - A- can donate to A-, A+, AB-, AB+.
     * - A+ can donate to A+, AB+.
     * - B- can donate to B-, B+, AB-, AB+.
     * - B+ can donate to B+, AB+.
     * - AB- can donate to AB-, AB+.
     * - AB+ can donate to AB+.
     */
    fun isMatch(donor: BloodType, patient: BloodType): Boolean {
        val compatiblePatients = when (donor) {
            BloodType.O_NEGATIVE -> listOf(
                BloodType.O_NEGATIVE, BloodType.O_POSITIVE,
                BloodType.A_NEGATIVE, BloodType.A_POSITIVE,
                BloodType.B_NEGATIVE, BloodType.B_POSITIVE,
                BloodType.AB_NEGATIVE, BloodType.AB_POSITIVE
            )
            BloodType.O_POSITIVE -> listOf(
                BloodType.O_POSITIVE, BloodType.A_POSITIVE, 
                BloodType.B_POSITIVE, BloodType.AB_POSITIVE
            )
            BloodType.A_NEGATIVE -> listOf(
                BloodType.A_NEGATIVE, BloodType.A_POSITIVE,
                BloodType.AB_NEGATIVE, BloodType.AB_POSITIVE
            )
            BloodType.A_POSITIVE -> listOf(
                BloodType.A_POSITIVE, BloodType.AB_POSITIVE
            )
            BloodType.B_NEGATIVE -> listOf(
                BloodType.B_NEGATIVE, BloodType.B_POSITIVE,
                BloodType.AB_NEGATIVE, BloodType.AB_POSITIVE
            )
            BloodType.B_POSITIVE -> listOf(
                BloodType.B_POSITIVE, BloodType.AB_POSITIVE
            )
            BloodType.AB_NEGATIVE -> listOf(
                BloodType.AB_NEGATIVE, BloodType.AB_POSITIVE
            )
            BloodType.AB_POSITIVE -> listOf(
                BloodType.AB_POSITIVE
            )
        }
        
        return compatiblePatients.contains(patient)
    }
}
