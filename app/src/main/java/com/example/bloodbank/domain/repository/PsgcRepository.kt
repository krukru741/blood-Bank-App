package com.example.bloodbank.domain.repository

import com.example.bloodbank.data.remote.api.dto.PsgcLocationDto

interface PsgcRepository {
    suspend fun getProvinces(): List<PsgcLocationDto>
    suspend fun getCitiesByProvince(provinceCode: String): List<PsgcLocationDto>
    suspend fun getBarangaysByCity(cityCode: String): List<PsgcLocationDto>
}
