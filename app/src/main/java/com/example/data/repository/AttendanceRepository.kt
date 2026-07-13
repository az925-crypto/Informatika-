package com.example.data.repository

import android.location.Location
import com.example.data.database.UserDao
import com.example.data.database.AttendanceDao
import com.example.data.database.AdminDao
import com.example.data.database.AppConfigDao
import com.example.data.model.Admin
import com.example.data.model.AppConfig
import com.example.data.model.AttendanceRecord
import com.example.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.*

class AttendanceRepository(
    private val userDao: UserDao,
    private val attendanceDao: AttendanceDao,
    private val adminDao: AdminDao,
    private val appConfigDao: AppConfigDao
) {
    val allUsers: Flow<List<User>> = userDao.getAllUsers()
    val allRecords: Flow<List<AttendanceRecord>> = attendanceDao.getAllRecords()
    val configFlow: Flow<AppConfig?> = appConfigDao.getConfigFlow()

    suspend fun initDefaultData() {
        // Seed default config if none
        if (appConfigDao.getConfig() == null) {
            appConfigDao.saveConfig(AppConfig())
        }
        // Seed default admin if none
        if (adminDao.getAdminByUsername("admin") == null) {
            adminDao.insertAdmin(Admin(username = "admin", passwordHash = "admin123"))
        }
        // Seed some sample users if empty
        val users = userDao.getAllUsers().firstOrNull()
        if (users.isNullOrEmpty()) {
            userDao.insertUser(User(name = "Ahmad Faisal", uniqueCode = "FAISAL123", role = "Karyawan", jabatan = "IT Support"))
            userDao.insertUser(User(name = "Budi Hartono", uniqueCode = "BUDI456", role = "Karyawan", jabatan = "HRD"))
            userDao.insertUser(User(name = "Siti Aminah", uniqueCode = "SITI789", role = "Siswa", jabatan = "XI-IPA"))
        }
    }

    suspend fun getUserByCode(code: String): User? = userDao.getUserByCode(code)
    suspend fun getUserById(id: Long): User? = userDao.getUserById(id)
    suspend fun insertUser(user: User): Long = userDao.insertUser(user)
    suspend fun updateUser(user: User) = userDao.updateUser(user)
    suspend fun deleteUser(user: User) = userDao.deleteUser(user)

    suspend fun getRecordForUserOnDate(userId: Long, date: String): AttendanceRecord? =
        attendanceDao.getRecordForUserOnDate(userId, date)

    suspend fun insertRecord(record: AttendanceRecord): Long = attendanceDao.insertRecord(record)
    suspend fun updateRecord(record: AttendanceRecord) = attendanceDao.updateRecord(record)
    suspend fun deleteRecord(record: AttendanceRecord) = attendanceDao.deleteRecord(record)

    suspend fun getAdminByUsername(username: String): Admin? = adminDao.getAdminByUsername(username)

    suspend fun saveConfig(config: AppConfig) = appConfigDao.saveConfig(config)
    suspend fun getConfig(): AppConfig? = appConfigDao.getConfig()

    fun getRecordsForUser(userId: Long): Flow<List<AttendanceRecord>> =
        attendanceDao.getRecordsForUser(userId)

    fun getRecordsForDate(date: String): Flow<List<AttendanceRecord>> =
        attendanceDao.getRecordsForDate(date)

    // Core business logic for scanning QR
    suspend fun processScan(
        code: String,
        latitude: Double? = null,
        longitude: Double? = null
    ): ScanResult {
        val user = userDao.getUserByCode(code) ?: return ScanResult.Error("User dengan kode \"$code\" tidak terdaftar!")

        val config = appConfigDao.getConfig() ?: AppConfig()

        // Geofencing verification
        if (config.isGeofencingEnabled) {
            if (latitude == null || longitude == null) {
                return ScanResult.Error("Gagal mendapatkan lokasi GPS Anda untuk validasi geofencing!")
            }
            val results = FloatArray(1)
            Location.distanceBetween(
                latitude, longitude,
                config.officeLatitude, config.officeLongitude,
                results
            )
            val distance = results[0]
            if (distance > config.officeRadiusMeters) {
                return ScanResult.Error("Gagal! Anda berada diluar jangkauan absen (${distance.toInt()}m dari lokasi kantor, radius max: ${config.officeRadiusMeters.toInt()}m)!")
            }
        }

        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentTimeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        val existingRecord = attendanceDao.getRecordForUserOnDate(user.id, todayDate)

        return if (existingRecord == null) {
            // Clock In (Absen Masuk)
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            val currentFormatted = currentTimeStr.substring(0, 5) // HH:mm
            val limitTimeCal = Calendar.getInstance().apply {
                time = format.parse(user.jamMasuk) ?: Date()
                add(Calendar.MINUTE, config.toleransiKeterlambatanMenit)
            }
            val currentTimeCal = Calendar.getInstance().apply {
                time = format.parse(currentFormatted) ?: Date()
            }

            val status = if (currentTimeCal.after(limitTimeCal)) "Terlambat" else "Hadir"

            val record = AttendanceRecord(
                userId = user.id,
                date = todayDate,
                clockInTime = currentTimeStr,
                clockOutTime = null,
                status = status,
                latitude = latitude,
                longitude = longitude
            )
            attendanceDao.insertRecord(record)
            ScanResult.SuccessClockIn(user, currentTimeStr, status)
        } else {
            // Clock Out (Absen Pulang)
            if (existingRecord.clockOutTime != null) {
                ScanResult.Error("Anda sudah melakukan absen masuk & pulang untuk hari ini!")
            } else {
                val updatedRecord = existingRecord.copy(
                    clockOutTime = currentTimeStr,
                    latitude = latitude ?: existingRecord.latitude,
                    longitude = longitude ?: existingRecord.longitude
                )
                attendanceDao.updateRecord(updatedRecord)
                ScanResult.SuccessClockOut(user, currentTimeStr)
            }
        }
    }
}

sealed class ScanResult {
    data class SuccessClockIn(val user: User, val time: String, val status: String) : ScanResult()
    data class SuccessClockOut(val user: User, val time: String) : ScanResult()
    data class Error(val message: String) : ScanResult()
}
