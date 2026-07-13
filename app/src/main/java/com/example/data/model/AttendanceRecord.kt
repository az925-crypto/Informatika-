package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val date: String, // Format YYYY-MM-DD
    val clockInTime: String? = null, // Format HH:mm:ss
    val clockOutTime: String? = null, // Format HH:mm:ss
    val status: String, // "Hadir", "Terlambat", "Sakit", "Izin", "Cuti", "Alpha"
    val notes: String? = null, // Keterangan manual
    val latitude: Double? = null,
    val longitude: Double? = null
)
