package com.example.bloodbank.data.remote.api

import com.example.bloodbank.data.remote.api.dto.PsgcLocationDto
import retrofit2.http.GET
import retrofit2.http.Path

interface PsgcApiService {

    @GET("provinces")
    suspend fun getProvinces(): List<PsgcLocationDto>

    @GET("provinces/{provinceCode}/cities-municipalities")
    suspend fun getCitiesByProvince(@Path("provinceCode") provinceCode: String): List<PsgcLocationDto>

    @GET("cities-municipalities/{cityCode}/barangays")
    suspend fun getBarangaysByCity(@Path("cityCode") cityCode: String): List<PsgcLocationDto>
}
