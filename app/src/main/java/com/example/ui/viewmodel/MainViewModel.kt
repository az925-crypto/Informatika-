package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.AppConfig
import com.example.data.model.AttendanceRecord
import com.example.data.model.User
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.ScanResult
import com.example.utils.ExportHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = AttendanceRepository(
        db.userDao(),
        db.attendanceDao(),
        db.adminDao(),
        db.appConfigDao()
    )

    // Flow lists
    val users: StateFlow<List<User>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val records: StateFlow<List<AttendanceRecord>> = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val config: StateFlow<AppConfig> = repository.configFlow
        .map { it ?: AppConfig() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppConfig())

    // UI Session State
    private val _isAdminLoggedIn = MutableStateFlow(false)
    val isAdminLoggedIn: StateFlow<Boolean> = _isAdminLoggedIn.asStateFlow()

    private val _scanResultState = MutableStateFlow<ScanResult?>(null)
    val scanResultState: StateFlow<ScanResult?> = _scanResultState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initDefaultData()
        }
    }

    fun loginAdmin(username: String, passwordRaw: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val admin = repository.getAdminByUsername(username)
            if (admin != null && admin.passwordHash == passwordRaw) {
                _isAdminLoggedIn.value = true
                onSuccess()
            } else {
                onError("Username atau password salah!")
            }
        }
    }

    fun logoutAdmin() {
        _isAdminLoggedIn.value = false
    }

    // CRUD User
    fun addUser(user: User) {
        viewModelScope.launch {
            repository.insertUser(user)
        }
    }

    fun updateUser(user: User) {
        viewModelScope.launch {
            repository.updateUser(user)
        }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch {
            repository.deleteUser(user)
        }
    }

    // Manual status override (Sakit, Izin, Cuti, Alpha)
    fun setManualAttendance(user: User, status: String, notes: String, date: String) {
        viewModelScope.launch {
            val existing = repository.getRecordForUserOnDate(user.id, date)
            if (existing == null) {
                val record = AttendanceRecord(
                    userId = user.id,
                    date = date,
                    clockInTime = null,
                    clockOutTime = null,
                    status = status,
                    notes = notes
                )
                repository.insertRecord(record)
            } else {
                val updated = existing.copy(
                    status = status,
                    notes = notes
                )
                repository.updateRecord(updated)
            }
        }
    }

    // Scan Processing
    fun processScan(code: String, latitude: Double? = null, longitude: Double? = null) {
        viewModelScope.launch {
            val result = repository.processScan(code, latitude, longitude)
            _scanResultState.value = result
        }
    }

    fun clearScanResult() {
        _scanResultState.value = null
    }

    // Save System Config
    fun saveConfig(newConfig: AppConfig) {
        viewModelScope.launch {
            repository.saveConfig(newConfig)
        }
    }

    // Report Exporters
    fun exportCsv(context: Context, onResult: (File?) -> Unit) {
        viewModelScope.launch {
            val file = ExportHelper.exportToCsv(context, records.value, users.value)
            onResult(file)
        }
    }

    fun exportPdf(context: Context, onResult: (File?) -> Unit) {
        viewModelScope.launch {
            val file = ExportHelper.exportToPdf(context, records.value, users.value)
            onResult(file)
        }
    }
}
