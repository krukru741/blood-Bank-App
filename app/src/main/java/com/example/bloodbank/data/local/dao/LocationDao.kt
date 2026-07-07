package com.example.bloodbank.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.bloodbank.data.local.entity.PsgcLocationEntity

@Dao
interface LocationDao {
    
    @Query("SELECT * FROM psgc_locations WHERE type = 'PROVINCE' ORDER BY name ASC")
    suspend fun getProvinces(): List<PsgcLocationEntity>

    @Query("SELECT * FROM psgc_locations WHERE type = 'CITY' AND parentCode = :provinceCode ORDER BY name ASC")
    suspend fun getCitiesByProvince(provinceCode: String): List<PsgcLocationEntity>

    @Query("SELECT * FROM psgc_locations WHERE type = 'BARANGAY' AND parentCode = :cityCode ORDER BY name ASC")
    suspend fun getBarangaysByCity(cityCode: String): List<PsgcLocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<PsgcLocationEntity>)
    
    @Query("DELETE FROM psgc_locations WHERE type = :type")
    suspend fun deleteLocationsByType(type: String)
}
