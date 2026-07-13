package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_config")
data class AppConfig(
    @PrimaryKey val id: Int = 1,
    val defaultJamMasuk: String = "08:00",
    val defaultJamPulang: String = "17:00",
    val toleransiKeterlambatanMenit: Int = 15,
    val isGeofencingEnabled: Boolean = false,
    val officeLatitude: Double = -6.2000, // Default Jakarta coordinates
    val officeLongitude: Double = 106.8166,
    val officeRadiusMeters: Double = 100.0
)
