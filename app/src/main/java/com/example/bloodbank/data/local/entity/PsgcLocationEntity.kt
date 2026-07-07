package com.example.bloodbank.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.bloodbank.data.remote.api.dto.PsgcLocationDto

@Entity(tableName = "psgc_locations")
data class PsgcLocationEntity(
    @PrimaryKey
    val code: String,
    val name: String,
    val type: String, // "PROVINCE", "CITY", "BARANGAY"
    val parentCode: String? = null // e.g., provinceCode for cities, cityCode for barangays
) {
    fun toDto(): PsgcLocationDto {
        return PsgcLocationDto(
            code = code,
            name = name
        )
    }
}
