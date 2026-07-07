package com.example.bloodbank.data.repository

import com.example.bloodbank.data.local.dao.LocationDao
import com.example.bloodbank.data.local.entity.PsgcLocationEntity
import com.example.bloodbank.data.remote.api.PsgcApiService
import com.example.bloodbank.data.remote.api.dto.PsgcLocationDto
import com.example.bloodbank.domain.repository.PsgcRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PsgcRepositoryImpl @Inject constructor(
    private val apiService: PsgcApiService,
    private val locationDao: LocationDao
) : PsgcRepository {

    override suspend fun getProvinces(): List<PsgcLocationDto> {
        // 1. Try fetching from Room cache
        val localProvinces = locationDao.getProvinces()
        if (localProvinces.isNotEmpty()) {
            return localProvinces.map { it.toDto() }
        }

        // 2. Fallback to API if cache is empty
        val remoteProvinces = apiService.getProvinces()
        
        // 3. Cache the remote result
        val entities = remoteProvinces.map {
            PsgcLocationEntity(
                code = it.code,
                name = it.name,
                type = "PROVINCE"
            )
        }
        locationDao.insertLocations(entities)

        return remoteProvinces
    }

    override suspend fun getCitiesByProvince(provinceCode: String): List<PsgcLocationDto> {
        val localCities = locationDao.getCitiesByProvince(provinceCode)
        if (localCities.isNotEmpty()) {
            return localCities.map { it.toDto() }
        }

        val remoteCities = apiService.getCitiesByProvince(provinceCode)
        val entities = remoteCities.map {
            PsgcLocationEntity(
                code = it.code,
                name = it.name,
                type = "CITY",
                parentCode = provinceCode
            )
        }
        locationDao.insertLocations(entities)

        return remoteCities
    }

    override suspend fun getBarangaysByCity(cityCode: String): List<PsgcLocationDto> {
        val localBarangays = locationDao.getBarangaysByCity(cityCode)
        if (localBarangays.isNotEmpty()) {
            return localBarangays.map { it.toDto() }
        }

        val remoteBarangays = apiService.getBarangaysByCity(cityCode)
        val entities = remoteBarangays.map {
            PsgcLocationEntity(
                code = it.code,
                name = it.name,
                type = "BARANGAY",
                parentCode = cityCode
            )
        }
        locationDao.insertLocations(entities)

        return remoteBarangays
    }
}
