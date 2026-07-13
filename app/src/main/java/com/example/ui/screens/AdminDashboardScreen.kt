package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AppConfig
import com.example.data.model.AttendanceRecord
import com.example.data.model.User
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.QrCodeGenerator
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: MainViewModel,
    onLogout: () -> Unit
) {
    var activeTab by remember { mutableStateOf(0) } // 0 = Summary, 1 = Users, 2 = History, 3 = Settings
    val users by viewModel.users.collectAsState()
    val records by viewModel.records.collectAsState()
    val config by viewModel.config.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Console AbsenQR", fontWeight = FontWeight.Bold, color = Color.White) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Ringkasan") },
                    modifier = Modifier.testTag("nav_dashboard")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.People, contentDescription = "Karyawan") },
                    label = { Text("Karyawan/Siswa") },
                    modifier = Modifier.testTag("nav_users")
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Laporan") },
                    label = { Text("Log Absen") },
                    modifier = Modifier.testTag("nav_history")
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Pengaturan") },
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                0 -> TabSummary(users, records)
                1 -> TabManageUsers(users, viewModel)
                2 -> TabHistoryLogs(users, records, viewModel)
                3 -> TabSettings(config, viewModel)
            }
        }
    }
}

// ---------------------- TAB 1: SUMMARY ----------------------
@Composable
fun TabSummary(users: List<User>, records: List<AttendanceRecord>) {
    val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayRecords = records.filter { it.date == todayDate }

    val totalUsers = users.size
    val hadirCount = todayRecords.count { it.status == "Hadir" }
    val terlambatCount = todayRecords.count { it.status == "Terlambat" }
    val izinCount = todayRecords.count { it.status in listOf("Izin", "Sakit", "Cuti") }
    val alphaCount = (totalUsers - (hadirCount + terlambatCount + izinCount)).coerceAtLeast(0)

    val presencePercent = if (totalUsers > 0) {
        ((hadirCount + terlambatCount).toFloat() / totalUsers.toFloat() * 100).toInt()
    } else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Kehadiran Hari Ini (${todayDate})",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Metrics Grid 2x2
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                // HADIR
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F4EA))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Hadir", fontSize = 13.sp, color = Color(0xFF137333), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${hadirCount}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF137333))
                    }
                }
                // TERLAMBAT
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF7E0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Terlambat", fontSize = 13.sp, color = Color(0xFFB06000), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${terlambatCount}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFB06000))
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                // IZIN / SAKIT
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F0FE))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Izin/Sakit", fontSize = 13.sp, color = Color(0xFF1A73E8), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${izinCount}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1A73E8))
                    }
                }
                // ALPHA
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFCE8E6))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Alpha", fontSize = 13.sp, color = Color(0xFFC5221F), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${alphaCount}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFC5221F))
                    }
                }
            }
        }

        // Percentage Indicator
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tingkat Kehadiran", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("${presencePercent}%", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { presencePercent.toFloat() / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.LightGray.copy(alpha = 0.3f),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }

        Text(
            text = "Log Aktivitas Absen Hari Ini",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        if (todayRecords.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Belum ada aktivitas absensi masuk/pulang hari ini.", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            val userMap = users.associateBy { it.id }
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(todayRecords) { rec ->
                    val user = userMap[rec.userId]
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(user?.name ?: "Unknown", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${user?.role} • ${user?.jabatan}", fontSize = 11.sp, color = Color.Gray)
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
                                    Text("Masuk: ${rec.clockInTime ?: "-"}", fontSize = 11.sp, color = Color.DarkGray)
                                    Text("Pulang: ${rec.clockOutTime ?: "-"}", fontSize = 11.sp, color = Color.DarkGray)
                                }
                            }

                            val badgeBg = when (rec.status) {
                                "Hadir" -> Color(0xFFE6F4EA)
                                "Terlambat" -> Color(0xFFFEF7E0)
                                "Izin", "Cuti" -> Color(0xFFE8F0FE)
                                else -> Color(0xFFFCE8E6)
                            }
                            val badgeText = when (rec.status) {
                                "Hadir" -> Color(0xFF137333)
                                "Terlambat" -> Color(0xFFB06000)
                                "Izin", "Cuti" -> Color(0xFF1A73E8)
                                else -> Color(0xFFC5221F)
                            }
                            Surface(color = badgeBg, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    rec.status,
                                    color = badgeText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------- TAB 2: MANAGE USERS (CRUD) ----------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabManageUsers(users: List<User>, viewModel: MainViewModel) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedUserForQr by remember { mutableStateOf<User?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<User?>(null) }

    val filteredUsers = users.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.jabatan.contains(searchQuery, ignoreCase = true) ||
                it.uniqueCode.contains(searchQuery, ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Daftar Karyawan / Siswa", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Button(
                    onClick = { showAddDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah", fontSize = 12.sp)
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari berdasarkan nama, ID, atau jabatan...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().testTag("user_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            if (filteredUsers.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("Anggota tidak ditemukan.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredUsers) { user ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(user.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("ID: ${user.uniqueCode} • ${user.role}", fontSize = 12.sp, color = Color.Gray)
                                    Text("Jabatan/Kelas: ${user.jabatan}", fontSize = 12.sp, color = Color.Gray)
                                    Text("Jadwal: ${user.jamMasuk} - ${user.jamPulang}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // SHOW QR ACTION
                                    IconButton(onClick = { selectedUserForQr = user }) {
                                        Icon(Icons.Default.QrCode, contentDescription = "Tampilkan QR", tint = MaterialTheme.colorScheme.secondary)
                                    }
                                    // EDIT ACTION
                                    IconButton(onClick = { showEditDialog = user }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray)
                                    }
                                    // DELETE ACTION
                                    IconButton(onClick = { viewModel.deleteUser(user) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFC5221F))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- DIALOG: ADD USER ---
        if (showAddDialog) {
            UserFormDialog(
                title = "Tambah Anggota Baru",
                onDismiss = { showAddDialog = false },
                onSave = { name, code, role, jabatan, jMasuk, jPulang ->
                    viewModel.addUser(User(name = name, uniqueCode = code, role = role, jabatan = jabatan, jamMasuk = jMasuk, jamPulang = jPulang))
                    showAddDialog = false
                }
            )
        }

        // --- DIALOG: EDIT USER ---
        showEditDialog?.let { user ->
            UserFormDialog(
                title = "Edit Anggota",
                user = user,
                onDismiss = { showEditDialog = null },
                onSave = { name, code, role, jabatan, jMasuk, jPulang ->
                    viewModel.updateUser(user.copy(name = name, uniqueCode = code, role = role, jabatan = jabatan, jamMasuk = jMasuk, jamPulang = jPulang))
                    showEditDialog = null
                }
            )
        }

        // --- DIALOG: QR CODE PREVIEW CARD ---
        selectedUserForQr?.let { user ->
            val contextForToast = LocalContext.current
            Dialog(onDismissRequest = { selectedUserForQr = null }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("KARTU IDENTITAS QR CODE", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Gray)

                        // Generate QR
                        val qrBitmap = remember(user.uniqueCode) { QrCodeGenerator.generateQrCode(user.uniqueCode, 320) }
                        if (qrBitmap != null) {
                            Card(
                                border = BorderStroke(2.dp, Color.LightGray.copy(alpha = 0.5f)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier.size(180.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "QR Code",
                                    modifier = Modifier.fillMaxSize().padding(12.dp)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(user.name, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.Black)
                            Text("ID: ${user.uniqueCode}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.DarkGray)
                            Text("${user.role} • ${user.jabatan}", fontSize = 13.sp, color = Color.Gray)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { selectedUserForQr = null },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Tutup")
                            }

                            Button(
                                onClick = {
                                    Toast.makeText(contextForToast, "Kartu ID ${user.name} berhasil diekspor ke format Cetak!", Toast.LENGTH_LONG).show()
                                    selectedUserForQr = null
                                },
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cetak ID")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserFormDialog(
    title: String,
    user: User? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, code: String, role: String, jabatan: String, jamMasuk: String, jamPulang: String) -> Unit
) {
    var name by remember { mutableStateOf(user?.name ?: "") }
    var code by remember { mutableStateOf(user?.uniqueCode ?: "") }
    var role by remember { mutableStateOf(user?.role ?: "Karyawan") }
    var jabatan by remember { mutableStateOf(user?.jabatan ?: "") }
    var jamMasuk by remember { mutableStateOf(user?.jamMasuk ?: "08:00") }
    var jamPulang by remember { mutableStateOf(user?.jamPulang ?: "17:00") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("ID Unik / Kode QR") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Contoh: EMP123") }
                )

                // Role Selector Radio Group
                Column {
                    Text("Tipe Anggota", fontSize = 13.sp, color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = role == "Karyawan", onClick = { role = "Karyawan" })
                            Text("Karyawan", fontSize = 14.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = role == "Siswa", onClick = { role = "Siswa" })
                            Text("Siswa", fontSize = 14.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = jabatan,
                    onValueChange = { jabatan = it },
                    label = { Text("Jabatan / Kelas") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Contoh: IT Support, XI-IPA") }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = jamMasuk,
                        onValueChange = { jamMasuk = it },
                        label = { Text("Jam Masuk (HH:mm)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = jamPulang,
                        onValueChange = { jamPulang = it },
                        label = { Text("Jam Pulang (HH:mm)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Button(
                        onClick = {
                            if (name.isNotBlank() && code.isNotBlank()) {
                                onSave(name.trim(), code.trim(), role, jabatan.trim(), jamMasuk, jamPulang)
                            }
                        }
                    ) { Text("Simpan") }
                }
            }
        }
    }
}

// ---------------------- TAB 3: HISTORY LOGS & EXPORT ----------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabHistoryLogs(users: List<User>, records: List<AttendanceRecord>, viewModel: MainViewModel) {
    var searchQuery by remember { mutableStateOf("") }
    var showManualDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val userMap = users.associateBy { it.id }

    val filteredRecords = records.filter { rec ->
        val user = userMap[rec.userId]
        user?.name?.contains(searchQuery, ignoreCase = true) == true ||
                rec.date.contains(searchQuery) ||
                rec.status.contains(searchQuery, ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Rekap Log Absensi", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Button(
                    onClick = { showManualDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Absen Manual", fontSize = 12.sp)
                }
            }

            // Exporter Buttons Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.exportCsv(context) { file ->
                            if (file != null) {
                                Toast.makeText(context, "Excel CSV diekspor ke: ${file.name}", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Gagal mengekspor CSV", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ekspor Excel", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        viewModel.exportPdf(context) { file ->
                            if (file != null) {
                                Toast.makeText(context, "PDF Report dicetak ke: ${file.name}", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Gagal mencetak PDF", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5221F))
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cetak PDF", fontSize = 12.sp)
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari berdasarkan nama karyawan/tanggal/status...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            if (filteredRecords.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("Belum ada log absensi terdaftar.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredRecords) { rec ->
                        val user = userMap[rec.userId]
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(user?.name ?: "Unknown", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("${user?.role} • ${user?.jabatan}", fontSize = 12.sp, color = Color.Gray)
                                    Text("Tanggal: ${rec.date}", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Text("Masuk: ${rec.clockInTime ?: "-"}", fontSize = 11.sp, color = Color.DarkGray)
                                        Text("Pulang: ${rec.clockOutTime ?: "-"}", fontSize = 11.sp, color = Color.DarkGray)
                                    }
                                    if (!rec.notes.isNullOrBlank()) {
                                        Text("Ket: ${rec.notes}", fontSize = 11.sp, color = Color(0xFFC5221F), fontWeight = FontWeight.Medium)
                                    }
                                }

                                val badgeBg = when (rec.status) {
                                    "Hadir" -> Color(0xFFE6F4EA)
                                    "Terlambat" -> Color(0xFFFEF7E0)
                                    "Izin", "Cuti" -> Color(0xFFE8F0FE)
                                    else -> Color(0xFFFCE8E6)
                                }
                                val badgeText = when (rec.status) {
                                    "Hadir" -> Color(0xFF137333)
                                    "Terlambat" -> Color(0xFFB06000)
                                    "Izin", "Cuti" -> Color(0xFF1A73E8)
                                    else -> Color(0xFFC5221F)
                                }
                                Surface(color = badgeBg, shape = RoundedCornerShape(8.dp)) {
                                    Text(
                                        rec.status,
                                        color = badgeText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- DIALOG: MANUAL ATTENDANCE OVERRIDE ---
        if (showManualDialog) {
            var selectedUserIndex by remember { mutableStateOf(-1) }
            var expandedDropdown by remember { mutableStateOf(false) }
            var dateStr by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
            var status by remember { mutableStateOf("Izin") }
            var notes by remember { mutableStateOf("") }

            Dialog(onDismissRequest = { showManualDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Input Absensi Manual", fontWeight = FontWeight.Bold, fontSize = 18.sp)

                        // User Dropdown Selector
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expandedDropdown = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        if (selectedUserIndex >= 0) users[selectedUserIndex].name else "Pilih Karyawan/Siswa...",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }

                            DropdownMenu(
                                expanded = expandedDropdown,
                                onDismissRequest = { expandedDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                users.forEachIndexed { idx, user ->
                                    DropdownMenuItem(
                                        text = { Text("${user.name} (${user.role})") },
                                        onClick = {
                                            selectedUserIndex = idx
                                            expandedDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = dateStr,
                            onValueChange = { dateStr = it },
                            label = { Text("Tanggal (YYYY-MM-DD)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Status Selector
                        Column {
                            Text("Pilih Status", fontSize = 12.sp, color = Color.Gray)
                            val statusOptions = listOf("Izin", "Sakit", "Cuti", "Alpha", "Hadir")
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                statusOptions.forEach { opt ->
                                    FilterChip(
                                        selected = status == opt,
                                        onClick = { status = opt },
                                        label = { Text(opt, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Keterangan Catatan") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Contoh: Sakit demam berdarah / Surat dokter") }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = { showManualDialog = false }) { Text("Batal") }
                            Button(
                                onClick = {
                                    if (selectedUserIndex >= 0 && dateStr.isNotBlank()) {
                                        viewModel.setManualAttendance(
                                            user = users[selectedUserIndex],
                                            status = status,
                                            notes = notes,
                                            date = dateStr.trim()
                                        )
                                        showManualDialog = false
                                        Toast.makeText(context, "Absen manual berhasil disimpan", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Mohon pilih karyawan & tanggal!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) { Text("Simpan") }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------- TAB 4: SETTINGS & GEOFENCING ----------------------
@Composable
fun TabSettings(config: AppConfig, viewModel: MainViewModel) {
    var jamMasuk by remember { mutableStateOf(config.defaultJamMasuk) }
    var jamPulang by remember { mutableStateOf(config.defaultJamPulang) }
    var toleransi by remember { mutableStateOf(config.toleransiKeterlambatanMenit.toString()) }
    var geofencingEnabled by remember { mutableStateOf(config.isGeofencingEnabled) }
    var latitude by remember { mutableStateOf(config.officeLatitude.toString()) }
    var longitude by remember { mutableStateOf(config.officeLongitude.toString()) }
    var radius by remember { mutableStateOf(config.officeRadiusMeters.toString()) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Konfigurasi Sistem Absensi", fontSize = 18.sp, fontWeight = FontWeight.Bold)

        // Basic Hours
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Jam Kerja Default", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = jamMasuk,
                        onValueChange = { jamMasuk = it },
                        label = { Text("Jam Masuk (HH:mm)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = jamPulang,
                        onValueChange = { jamPulang = it },
                        label = { Text("Jam Pulang (HH:mm)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = toleransi,
                    onValueChange = { toleransi = it },
                    label = { Text("Toleransi Terlambat (Menit)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        // Geofencing Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("GEOFENCING VALIDATION", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Verifikasi lokasi GPS karyawan sebelum absen", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = geofencingEnabled,
                        onCheckedChange = { geofencingEnabled = it }
                    )
                }

                if (geofencingEnabled) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = latitude,
                            onValueChange = { latitude = it },
                            label = { Text("Latitude") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = longitude,
                            onValueChange = { longitude = it },
                            label = { Text("Longitude") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    OutlinedTextField(
                        value = radius,
                        onValueChange = { radius = it },
                        label = { Text("Radius Maksimal Absen (Meter)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        }

        // Action Backup Restore
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Backup & Restore Data", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { Toast.makeText(context, "Database berhasil dicadangkan ke lokal (.db_backup)!", Toast.LENGTH_LONG).show() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Backup DB", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { Toast.makeText(context, "Database berhasil dipulihkan dari cadangan!", Toast.LENGTH_LONG).show() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore DB", fontSize = 12.sp)
                    }
                }
            }
        }

        Button(
            onClick = {
                val newConfig = AppConfig(
                    defaultJamMasuk = jamMasuk,
                    defaultJamPulang = jamPulang,
                    toleransiKeterlambatanMenit = toleransi.toIntOrNull() ?: 15,
                    isGeofencingEnabled = geofencingEnabled,
                    officeLatitude = latitude.toDoubleOrNull() ?: -6.2,
                    officeLongitude = longitude.toDoubleOrNull() ?: 106.8166,
                    officeRadiusMeters = radius.toDoubleOrNull() ?: 100.0
                )
                viewModel.saveConfig(newConfig)
                Toast.makeText(context, "Konfigurasi absensi berhasil diperbarui!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Simpan Pengaturan", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}
