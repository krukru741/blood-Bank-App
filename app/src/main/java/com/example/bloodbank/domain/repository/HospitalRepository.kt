package com.example.bloodbank.domain.repository

import com.example.bloodbank.core.utils.Resource
import com.example.bloodbank.domain.model.HospitalMarker
import kotlinx.coroutines.flow.Flow

interface HospitalRepository {
    fun getHospitals(): Flow<Resource<List<HospitalMarker>>>
    suspend fun seedHospitals(hospitals: List<HospitalMarker>): Resource<Unit>
}
