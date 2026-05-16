@file:Suppress("SpellCheckingInspection")
@file:OptIn(ExperimentalMaterial3Api::class)
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devcraft.jsmart.ui.theme.*
import com.devcraft.jsmart.data.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun AdminReportsScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Attendance", "Tasks", "Leave")

    var adminStats by remember { mutableStateOf(AdminReportStats()) }
    var isLoading by remember { mutableStateOf(true) }

    val now = remember {
        Clock.System.now().toLocalDateTime(
            TimeZone.currentSystemDefault()
        )
    }
    var fromDate by remember {
        mutableStateOf("${now.year}-${now.monthNumber
            .toString().padStart(2,'0')}-01")
    }
    var toDate by remember {
        mutableStateOf(now.date.toString())
    }
    var staffAttendance by remember { mutableStateOf<List<AttendanceDbRecord>>(emptyList()) }
    var allStaff by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(Unit) {
        adminStats = ReportsRepository.getAdminReportStats()
        val staffList = StaffRepository.getAllStaff()
        allStaff = staffList.associate { it.id to it.fullName }
        isLoading = false
    }

    LaunchedEffect(fromDate, toDate) {
        staffAttendance = ReportsRepository.getAllStaffAttendance(fromDate, toDate)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TealPrimary)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Analytics, contentDescription = null,
                tint = Cream, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("Reports & Analytics", color = Cream,
                fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }

        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = TealPrimary,
            contentColor = Cream
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(title, fontSize = 12.sp,
                            fontWeight = if (selectedTab == index)
                                FontWeight.SemiBold else FontWeight.Normal)
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = TealPrimary
                    )
                }
            } else {
                when (selectedTab) {
                    0 -> AdminOverviewTab(adminStats)
                    1 -> AdminAttendanceTab(fromDate, toDate, onFromDateChange = { fromDate = it }, onToDateChange = { toDate = it }, records = staffAttendance, staffMap = allStaff)
                    2 -> AdminTasksTab(adminStats)
                    3 -> AdminLeaveTab(adminStats)
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun AdminOverviewTab(stats: AdminReportStats) {
    Text("Daily Statistics", fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold, color = CharcoalDark)
    Spacer(modifier = Modifier.height(12.dp))

    // KPI grid
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AdminKpiCard("Present Today", stats.presentToday.toString(),
            Icons.Filled.CheckCircle, SuccessGreen, modifier = Modifier.weight(1f))
        AdminKpiCard("Absent Today", stats.absentToday.toString(),
            Icons.Filled.Cancel, ErrorRed, modifier = Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AdminKpiCard("Late Today", stats.lateToday.toString(),
            Icons.Filled.Schedule, WarningAmber, modifier = Modifier.weight(1f))
        AdminKpiCard("Pending Leaves", stats.pendingLeaveRequests.toString(),
            Icons.Filled.BeachAccess, TealPrimary, modifier = Modifier.weight(1f))
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text("Task Overview", fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold, color = CharcoalDark)
    Spacer(modifier = Modifier.height(8.dp))

    QuickStatRow("Total Tasks Completed", stats.totalTasksCompleted.toString())
    QuickStatRow("Total Tasks Pending", stats.totalTasksPending.toString())
}

@Composable
fun AdminAttendanceTab(
    fromDate: String,
    toDate: String,
    onFromDateChange: (String) -> Unit,
    onToDateChange: (String) -> Unit,
    records: List<AttendanceDbRecord>,
    staffMap: Map<String, String>
) {
    Text("Attendance Filters", fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold, color = CharcoalDark)
    Spacer(modifier = Modifier.height(8.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = fromDate,
            onValueChange = onFromDateChange,
            label = { Text("From Date (YYYY-MM-DD)") },
            modifier = Modifier.weight(1f),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
        )
        OutlinedTextField(
            value = toDate,
            onValueChange = onToDateChange,
            label = { Text("To Date (YYYY-MM-DD)") },
            modifier = Modifier.weight(1f),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text("Staff Attendance List", fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold, color = CharcoalDark)
    Spacer(modifier = Modifier.height(8.dp))

    if (records.isEmpty()) {
        Text("No records in this range.", fontSize = 13.sp, color = CharcoalMedium)
    } else {
        records.forEach { record ->
            val staffName = staffMap[record.userId] ?: "Unknown Staff"
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(staffName, fontWeight = FontWeight.Bold, color = CharcoalDark)
                        StatusBadge(record.status.replaceFirstChar { it.uppercase() })
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(record.date, fontSize = 12.sp, color = CharcoalMedium)
                        Text("${record.clockInTime ?: "--"} - ${record.clockOutTime ?: "--"}", fontSize = 12.sp, color = CharcoalMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminTasksTab(stats: AdminReportStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TealPrimary)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ReportStat("Completed", stats.totalTasksCompleted.toString(), SuccessGreen)
            ReportStat("Pending", stats.totalTasksPending.toString(), WarningAmber)
        }
    }

    Spacer(modifier = Modifier.height(20.dp))
    Text("Tasks analytics are visible in the Overview tab.", fontSize = 13.sp, color = CharcoalMedium)
}

@Composable
fun AdminLeaveTab(stats: AdminReportStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TealPrimary)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ReportStat("Pending Leave Requests", stats.pendingLeaveRequests.toString(), WarningAmber)
        }
    }
}

// ── Supporting composables ──────────────────────────────────────────

@Composable
fun AdminKpiCard(
    label: String, value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null,
                    tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(value, fontSize = 22.sp,
                    fontWeight = FontWeight.Bold, color = color)
                Text(label, fontSize = 11.sp, color = CharcoalMedium)
            }
        }
    }
}

@Composable
fun BranchAttendanceCard(branch: String, present: Int, total: Int) {
    val pct = (present.toFloat() / total * 100).toInt()
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(branch, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, color = CharcoalDark)
                Text("$present / $total  •  $pct%",
                    fontSize = 13.sp, color = TealPrimary,
                    fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { present.toFloat() / total },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = TealPrimary,
                trackColor = TealPrimary.copy(alpha = 0.15f)
            )
        }
    }
}

@Composable
fun QuickStatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = CharcoalMedium)
        Text(value, fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold, color = CharcoalDark)
    }
    HorizontalDivider(color = CharcoalMedium.copy(alpha = 0.1f))
}

data class StaffAttendanceSummary(
    val name: String, val present: Int, val late: Int, val absent: Int
)

@Composable
fun StaffAttendanceSummaryCard(summary: StaffAttendanceSummary) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(summary.name, fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = CharcoalDark, modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat("${summary.present}P", SuccessGreen)
                MiniStat("${summary.late}L", WarningAmber)
                MiniStat("${summary.absent}A", ErrorRed)
            }
        }
    }
}

@Composable
fun MiniStat(label: String, color: androidx.compose.ui.graphics.Color) {
    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
}

@Composable
fun TaskCompletionRow(name: String, assigned: Int, done: Int) {
    val pct = (done.toFloat() / assigned * 100).toInt()
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(name, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, color = CharcoalDark)
                Text("$done/$assigned  •  $pct%",
                    fontSize = 12.sp, color = TealPrimary,
                    fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { done.toFloat() / assigned },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = TealPrimary,
                trackColor = TealPrimary.copy(alpha = 0.15f)
            )
        }
    }
}

@Composable
fun LeaveByStaffRow(name: String, detail: String, type: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, color = CharcoalDark)
                Text(detail, fontSize = 11.sp, color = CharcoalMedium)
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = TealPrimary.copy(alpha = 0.12f)
            ) {
                Text(type, color = TealPrimary,
                    fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
            }
        }
    }
}