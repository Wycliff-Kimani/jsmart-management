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
import com.devcraft.jsmart.UserSession

@Composable
fun MyReportsScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Attendance", "Tasks", "Leave")

    var attendanceSummary by remember {
        mutableStateOf(GlobalReportsAttendanceSummary())
    }
    var tasksSummary by remember {
        mutableStateOf(TasksSummary())
    }
    var leaveHistory by remember {
        mutableStateOf<List<LeaveRow>>(emptyList())
    }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val userId = UserSession.get()?.id ?: return@LaunchedEffect
        attendanceSummary = ReportsRepository
            .getMyAttendanceSummary(userId)
        tasksSummary = ReportsRepository
            .getMyTasksSummary(userId)
        leaveHistory = ReportsRepository
            .getMyLeaveHistory(userId)
        isLoading = false
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
            Icon(Icons.Filled.BarChart, contentDescription = null,
                tint = Cream, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("My Reports", color = Cream,
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
                        Text(title, fontSize = 13.sp,
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
                    CircularProgressIndicator(color = TealPrimary)
                }
            } else {
                when (selectedTab) {
                    0 -> MyAttendanceReport(attendanceSummary)
                    1 -> MyTasksReport(tasksSummary)
                    2 -> MyLeaveReport(leaveHistory)
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun MyAttendanceReport(summary: GlobalReportsAttendanceSummary) {
    // Monthly summary card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TealPrimary)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Current Month Summary", color = TealLight,
                fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ReportStat("Present", summary.present.toString(), Cream)
                ReportStat("Absent", summary.absent.toString(), ErrorRed)
                ReportStat("Late", summary.late.toString(), WarningAmber)
                ReportStat("Rate", "${summary.attendanceRate}%", TealLight)
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text("This Month's Attendance", fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold, color = CharcoalDark)
    Spacer(modifier = Modifier.height(8.dp))

    val records = summary.records
    if (records.isEmpty()) {
        Text("No attendance records found.", fontSize = 13.sp, color = CharcoalMedium)
    } else {
        Column {
            records.forEach { record ->
                AttendanceReportRow(
                    record.date,
                    "${record.clockInTime ?: "--"} – ${record.clockOutTime ?: "--"}",
                    record.status.replaceFirstChar { it.uppercase() }
                )
            }
        }
    }
}

@Composable
fun MyTasksReport(summary: TasksSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TealPrimary)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Task Performance", color = TealLight,
                fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ReportStat("Total", summary.total.toString(), Cream)
                ReportStat("Done", summary.completed.toString(), SuccessGreen)
                ReportStat("Pending", summary.pending.toString(), WarningAmber)
                ReportStat("Overdue", summary.overdue.toString(), ErrorRed)
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text("Recent Tasks", fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold, color = CharcoalDark)
    Spacer(modifier = Modifier.height(8.dp))

    if (summary.tasks.isEmpty()) {
        Text("No tasks assigned.", fontSize = 13.sp, color = CharcoalMedium)
    } else {
        summary.tasks.forEach { task ->
            TaskReportRow(
                task.title,
                task.dueDate ?: "No due date",
                task.status.replaceFirstChar { it.uppercase() }
            )
        }
    }
}

@Composable
fun MyLeaveReport(history: List<LeaveRow>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TealPrimary)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Leave Statistics", color = TealLight,
                fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ReportStat("Total", history.size.toString(), Cream)
                ReportStat("Approved", history.count { it.status == "approved" }.toString(), SuccessGreen)
                ReportStat("Pending", history.count { it.status == "pending" }.toString(), WarningAmber)
                ReportStat("Rejected", history.count { it.status == "rejected" }.toString(), ErrorRed)
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text("Leave History", fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold, color = CharcoalDark)
    Spacer(modifier = Modifier.height(8.dp))

    if (history.isEmpty()) {
        Text("No leave history", fontSize = 13.sp, color = CharcoalMedium)
    } else {
        history.forEach { leave ->
            LeaveReportRow(
                leave.leaveType,
                "${leave.startDate} to ${leave.endDate}",
                leave.status.replaceFirstChar { it.uppercase() }
            )
        }
    }
}

@Composable
fun ReportStat(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TealLight, fontSize = 10.sp)
    }
}

@Composable
fun AttendanceReportRow(date: String, hours: String, status: String) {
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
                Text(date, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, color = CharcoalDark)
                Text(hours, fontSize = 11.sp, color = CharcoalMedium)
            }
            StatusBadge(status)
        }
    }
}

@Composable
fun TaskReportRow(task: String, date: String, status: String) {
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
                Text(task, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, color = CharcoalDark)
                Text("Due $date", fontSize = 11.sp, color = CharcoalMedium)
            }
            StatusBadge(status)
        }
    }
}

@Composable
fun LeaveReportRow(type: String, detail: String, status: String) {
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
                Text(type, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, color = CharcoalDark)
                Text(detail, fontSize = 11.sp, color = CharcoalMedium)
            }
            StatusBadge(status)
        }
    }
}