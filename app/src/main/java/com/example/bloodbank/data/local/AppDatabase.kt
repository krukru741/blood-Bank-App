package com.example.bloodbank.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.bloodbank.data.local.dao.LocationDao
import com.example.bloodbank.data.local.entity.PsgcLocationEntity

@Database(
    entities = [PsgcLocationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract val locationDao: LocationDao
}
