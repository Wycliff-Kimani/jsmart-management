@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.devcraft.jsmart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devcraft.jsmart.data.AttendanceDbRecord
import com.devcraft.jsmart.data.AttendanceRepository
import com.devcraft.jsmart.data.AttendanceSummary
import com.devcraft.jsmart.ui.theme.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AttendanceHistoryScreen() {
    var records by remember { mutableStateOf<List<AttendanceDbRecord>>(emptyList()) }
    var summary by remember { mutableStateOf<AttendanceSummary?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            records = AttendanceRepository.getMyAttendanceRecords()
            summary = AttendanceRepository.getThisMonthSummary()
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TealPrimary)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = null,
                tint = Cream, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("Attendance History", color = Cream,
                fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TealPrimary)
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Monthly summary card
            val s = summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = TealPrimary)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val monthLabel = java.time.LocalDate.now()
                        .format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                    Text(monthLabel, color = TealLight,
                        fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MonthlyStat("Attendance", "${s?.attendanceRate ?: 0}%")
                        MonthlyStat("Present", "${s?.presentDays ?: 0}")
                        MonthlyStat("Late", "${s?.lateDays ?: 0}")
                        MonthlyStat("Absent", "${s?.absentDays ?: 0}")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Records list
            Text("Recent Records", fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold, color = CharcoalDark)
            Spacer(modifier = Modifier.height(8.dp))

            if (records.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No attendance records yet",
                        color = CharcoalMedium, fontSize = 14.sp,
                        textAlign = TextAlign.Center)
                }
            } else {
                records.forEach { record ->
                    AttendanceRecordRow(record)
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}


@Composable
fun AttendanceRecordRow(record: AttendanceDbRecord) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(formatDate(record.date), fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, color = CharcoalDark)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Login, contentDescription = null,
                            tint = SuccessGreen, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(formatTime(record.clockInTime),
                            fontSize = 12.sp, color = CharcoalMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Logout, contentDescription = null,
                            tint = ErrorRed, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(formatTime(record.clockOutTime),
                            fontSize = 12.sp, color = CharcoalMedium)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(calculateHours(record.clockInTime, record.clockOutTime),
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                StatusBadge(record.status.replaceFirstChar { it.uppercase() })
            }
        }
    }
}

@Composable
fun AttendanceHistoryRecord(
    date: String, clockIn: String, clockOut: String,
    hours: String, status: String
) {} // kept for compatibility, not used

@Composable
fun StatusBadge(status: String) {
    val color = when (status.lowercase()) {
        "present" -> SuccessGreen
        "late" -> WarningAmber
        "absent", "on_leave" -> ErrorRed
        else -> CharcoalMedium
    }
    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.12f)) {
        Text(status, color = color, fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@Composable
fun MonthlyStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Cream, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TealLight, fontSize = 10.sp)
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp)
            .background(color, RoundedCornerShape(3.dp)))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, color = CharcoalMedium)
    }
}