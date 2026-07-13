package com.example.data.database

import androidx.room.*
import com.example.data.model.AttendanceRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records ORDER BY date DESC, clockInTime DESC")
    fun getAllRecords(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE userId = :userId ORDER BY date DESC")
    fun getRecordsForUser(userId: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getRecordForUserOnDate(userId: Long, date: String): AttendanceRecord?

    @Query("SELECT * FROM attendance_records WHERE date = :date")
    fun getRecordsForDate(date: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE date BETWEEN :startDate AND :endDate")
    fun getRecordsBetweenDates(startDate: String, endDate: String): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecord): Long

    @Update
    suspend fun updateRecord(record: AttendanceRecord)

    @Delete
    suspend fun deleteRecord(record: AttendanceRecord)
}
