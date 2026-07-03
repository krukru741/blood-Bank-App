package com.example.bloodbank.data.remote.mapper

import com.example.bloodbank.data.remote.dto.BloodRequestDto
import com.example.bloodbank.domain.model.BloodRequest
import com.example.bloodbank.domain.model.BloodType
import com.example.bloodbank.domain.model.RequestStatus
import com.example.bloodbank.domain.model.UrgencyLevel
import com.google.firebase.firestore.DocumentSnapshot

object BloodRequestMapper {

    fun mapToDomain(dto: BloodRequestDto): BloodRequest {
        return BloodRequest(
            requestId = dto.requestId,
            requesterId = dto.requesterId,
            requesterName = dto.requesterName,
            bloodType = BloodType.fromLabel(dto.bloodType),
            unitsNeeded = dto.unitsNeeded,
            hospital = dto.hospital,
            location = dto.location,
            latitude = dto.latitude,
            longitude = dto.longitude,
            urgency = runCatching { UrgencyLevel.valueOf(dto.urgency) }.getOrDefault(UrgencyLevel.NORMAL),
            status = runCatching { RequestStatus.valueOf(dto.status) }.getOrDefault(RequestStatus.PENDING),
            description = dto.description,
            contactNumber = dto.contactNumber,
            createdAt = dto.createdAt,
            expiresAt = dto.expiresAt,
            acceptedByDonorId = dto.acceptedByDonorId,
            acceptedByDonorName = dto.acceptedByDonorName,
            acceptedByDonorPhone = dto.acceptedByDonorPhone
        )
    }

    fun mapToDto(domain: BloodRequest): BloodRequestDto {
        return BloodRequestDto(
            requestId = domain.requestId,
            requesterId = domain.requesterId,
            requesterName = domain.requesterName,
            bloodType = domain.bloodType.label,
            unitsNeeded = domain.unitsNeeded,
            hospital = domain.hospital,
            location = domain.location,
            latitude = domain.latitude,
            longitude = domain.longitude,
            urgency = domain.urgency.name,
            status = domain.status.name,
            description = domain.description,
            contactNumber = domain.contactNumber,
            createdAt = domain.createdAt,
            expiresAt = domain.expiresAt,
            acceptedByDonorId = domain.acceptedByDonorId,
            acceptedByDonorName = domain.acceptedByDonorName,
            acceptedByDonorPhone = domain.acceptedByDonorPhone
        )
    }

    fun mapToDomain(document: DocumentSnapshot): BloodRequest? {
        if (!document.exists()) return null
        val dto = document.toObject(BloodRequestDto::class.java) ?: return null
        return mapToDomain(dto.copy(requestId = dto.requestId.ifEmpty { document.id }))
    }
}
