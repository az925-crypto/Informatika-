package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val uniqueCode: String, // kode_unik
    val photoUri: String? = null,
    val role: String, // e.g. "Karyawan", "Siswa"
    val jabatan: String, // e.g. "Staff", "Kelas 12"
    val jamMasuk: String = "08:00", // HH:mm
    val jamPulang: String = "17:00"  // HH:mm
)
