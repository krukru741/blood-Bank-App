package com.example.bloodbank.data.remote.mapper

import com.example.bloodbank.data.remote.dto.UserDto
import com.example.bloodbank.domain.model.BloodType
import com.example.bloodbank.domain.model.Gender
import com.example.bloodbank.domain.model.User
import com.example.bloodbank.domain.model.UserRole
import com.google.firebase.firestore.DocumentSnapshot

object UserMapper {

    fun mapToDomain(dto: UserDto): User {
        return User(
            uid = dto.uid,
            email = dto.email,
            displayName = dto.displayName,
            phoneNumber = dto.phoneNumber,
            bloodType = BloodType.fromLabel(dto.bloodType),
            gender = Gender.fromLabel(dto.gender),
            dateOfBirth = dto.dateOfBirth,
            role = runCatching { UserRole.valueOf(dto.role) }.getOrDefault(UserRole.USER),
            province = dto.province,
            city = dto.city,
            barangay = dto.barangay,
            street = dto.street,
            latitude = dto.latitude,
            longitude = dto.longitude,
            profilePhotoUrl = dto.profilePhotoUrl,
            isVerified = dto.isVerified,
            hasCreatedRequest = dto.hasCreatedRequest,
            createdAt = dto.createdAt,
            weightKg = dto.weightKg,
            lastDonationDate = dto.lastDonationDate,
            hospitalName = dto.hospitalName,
            hospitalAddress = dto.hospitalAddress
        )
    }

    fun mapToDto(domain: User): UserDto {
        return UserDto(
            uid = domain.uid,
            email = domain.email,
            displayName = domain.displayName,
            phoneNumber = domain.phoneNumber,
            bloodType = domain.bloodType.label,
            gender = domain.gender.label,
            dateOfBirth = domain.dateOfBirth,
            role = domain.role.name,
            province = domain.province,
            city = domain.city,
            barangay = domain.barangay,
            street = domain.street,
            latitude = domain.latitude,
            longitude = domain.longitude,
            profilePhotoUrl = domain.profilePhotoUrl,
            isVerified = domain.isVerified,
            hasCreatedRequest = domain.hasCreatedRequest,
            createdAt = domain.createdAt,
            weightKg = domain.weightKg,
            lastDonationDate = domain.lastDonationDate,
            hospitalName = domain.hospitalName,
            hospitalAddress = domain.hospitalAddress
        )
    }

    fun mapToDomain(document: DocumentSnapshot): User? {
        if (!document.exists()) return null
        val dto = document.toObject(UserDto::class.java) ?: return null
        // Ensure UID is set properly if it was missing from the document body
        return mapToDomain(dto.copy(uid = dto.uid.ifEmpty { document.id }))
    }
}
