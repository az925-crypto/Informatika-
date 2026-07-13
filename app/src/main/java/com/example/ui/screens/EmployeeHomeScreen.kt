package com.example.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.User
import com.example.data.repository.ScanResult
import com.example.ui.viewmodel.MainViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun EmployeeHomeScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var activeTab by remember { mutableStateOf(0) } // 0 = Scan QR, 1 = Riwayat Pribadi
    val users by viewModel.users.collectAsState()
    val records by viewModel.records.collectAsState()
    val scanResult by viewModel.scanResultState.collectAsState()

    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState("android.permission.CAMERA")

    // Trigger beep on scan results
    LaunchedEffect(scanResult) {
        if (scanResult != null) {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                if (scanResult is ScanResult.Error) {
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
                } else {
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Portal Absensi Mandiri", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Selector
            TabRow(selectedTabIndex = activeTab) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan QR")
                        }
                    },
                    modifier = Modifier.testTag("tab_scan_qr")
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Riwayat Absen")
                        }
                    },
                    modifier = Modifier.testTag("tab_riwayat_absen")
                )
            }

            if (activeTab == 0) {
                // SCAN TAB
                Box(modifier = Modifier.fillMaxSize()) {
                    if (cameraPermissionState.status.isGranted) {
                        CameraPreviewScanner { qrValue ->
                            viewModel.processScan(qrValue)
                        }

                        // Scanning Overlay Guide Frame
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    modifier = Modifier.size(260.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    color = Color.Transparent,
                                    border = BorderStroke(4.dp, MaterialTheme.colorScheme.primary)
                                ) {
                                    // Empty transparent interior
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Arahkan Kamera ke QR Code Anda",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        // Permission Request
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Izin Kamera Diperlukan",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Untuk melakukan absensi mandiri, aplikasi memerlukan akses ke kamera perangkat Anda untuk menscan QR Code.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { cameraPermissionState.launchPermissionRequest() },
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Berikan Izin Kamera")
                            }
                        }
                    }
                }
            } else {
                // RIWAYAT TAB (PERSONAL HISTORY LOOKUP)
                var selectedUser by remember { mutableStateOf<User?>(null) }
                var expandedDropdown by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Cari Riwayat Absensi Anda",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Dropdown Selector
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            onClick = { expandedDropdown = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = selectedUser?.name ?: "Pilih Nama Anda...",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedUser != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    if (selectedUser != null) {
                                        Text(
                                            text = "${selectedUser?.role} • ${selectedUser?.jabatan}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }

                        DropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            users.forEach { user ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(user.name, fontWeight = FontWeight.Bold)
                                            Text("${user.role} • ${user.jabatan}", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    },
                                    onClick = {
                                        selectedUser = user
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    if (selectedUser != null) {
                        val userRecords = records.filter { it.userId == selectedUser!!.id }

                        Text(
                            text = "Log Absensi (Total ${userRecords.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )

                        if (userRecords.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Belum ada riwayat absensi untuk user ini.",
                                    color = Color.Gray,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(userRecords) { rec ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = rec.date,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Masuk: ${rec.clockInTime ?: "-"}", fontSize = 12.sp, color = Color.Gray)
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Pulang: ${rec.clockOutTime ?: "-"}", fontSize = 12.sp, color = Color.Gray)
                                                    }
                                                }
                                            }

                                            val statusBg = when (rec.status) {
                                                "Hadir" -> Color(0xFFE6F4EA)
                                                "Terlambat" -> Color(0xFFFEF7E0)
                                                "Izin", "Cuti" -> Color(0xFFE8F0FE)
                                                else -> Color(0xFFFCE8E6)
                                            }
                                            val statusTextColor = when (rec.status) {
                                                "Hadir" -> Color(0xFF137333)
                                                "Terlambat" -> Color(0xFFB06000)
                                                "Izin", "Cuti" -> Color(0xFF1A73E8)
                                                else -> Color(0xFFC5221F)
                                            }

                                            Surface(
                                                color = statusBg,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = rec.status,
                                                    color = statusTextColor,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Empty state before selecting user
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.PersonSearch,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = Color.LightGray
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Pilih nama Anda di menu dropdown untuk melihat riwayat absensi pribadi.",
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // SCAN FEEDBACK DIALOG
        scanResult?.let { result ->
            Dialog(onDismissRequest = { viewModel.clearScanResult() }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (result) {
                            is ScanResult.SuccessClockIn -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = Color(0xFF137333),
                                    modifier = Modifier.size(72.dp)
                                )
                                Text(
                                    text = "ABSEN MASUK BERHASIL!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color(0xFF137333)
                                )
                                Text(
                                    text = result.user.name,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "${result.user.role} • ${result.user.jabatan}",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                                HorizontalDivider()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Waktu:", fontSize = 14.sp, color = Color.Gray)
                                    Text(result.time, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Status:", fontSize = 14.sp, color = Color.Gray)
                                    Text(
                                        result.status,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (result.status == "Terlambat") Color(0xFFB06000) else Color(0xFF137333)
                                    )
                                }
                            }
                            is ScanResult.SuccessClockOut -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = Color(0xFF1A73E8),
                                    modifier = Modifier.size(72.dp)
                                )
                                Text(
                                    text = "ABSEN PULANG BERHASIL!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color(0xFF1A73E8)
                                )
                                Text(
                                    text = result.user.name,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "${result.user.role} • ${result.user.jabatan}",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                                HorizontalDivider()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Waktu Pulang:", fontSize = 14.sp, color = Color.Gray)
                                    Text(result.time, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                            is ScanResult.Error -> {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = "Gagal",
                                    tint = Color(0xFFC5221F),
                                    modifier = Modifier.size(72.dp)
                                )
                                Text(
                                    text = "ABSENSI GAGAL!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color(0xFFC5221F)
                                )
                                Text(
                                    text = result.message,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.clearScanResult() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Tutup", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
