@file:Suppress("SpellCheckingInspection")
package com.devcraft.jsmart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devcraft.jsmart.UserSession
import com.devcraft.jsmart.navigation.Routes
import com.devcraft.jsmart.ui.theme.*
import androidx.compose.runtime.collectAsState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.devcraft.jsmart.data.*
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun AdminDashboardScreen(onNavigate: (String) -> Unit) {
    val userState = UserSession.currentUser.collectAsState()
    val user = userState.value
    val firstName = user?.fullName?.split(" ")?.firstOrNull() ?: "Admin"
    val initial = user?.fullName?.firstOrNull()?.toString() ?: "A"

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var stats by remember { mutableStateOf<AdminDashboardStats?>(null) }
    var staffList by remember { mutableStateOf<List<StaffAttendanceRow>>(emptyList()) }
    var pendingLeaves by remember { mutableStateOf<List<LeaveRow>>(emptyList()) }
    var selectedBranch by remember { mutableStateOf(user?.branchId ?: "965dc64f-9636-4062-88dc-a5c727f19245") }
    var isLoading by remember { mutableStateOf(true) }
    var isClockedIn by remember { mutableStateOf(false) }
    var isClockLoading by remember { mutableStateOf(false) }
    var unreadNotifCount by remember { mutableIntStateOf(0) }

    fun refreshData() {
        scope.launch {
            isLoading = true
            coroutineScope {
                val statsDef = async { AdminRepository.getDashboardStats(selectedBranch) }
                val staffDef = async { AdminRepository.getTodayStaffAttendance(selectedBranch) }
                val leavesDef = async { LeaveRepository.getPendingRequests() }
                val unreadCountDef = async { user?.id?.let { NotificationRepository.getUnreadCount(it) } ?: 0 }

                stats = statsDef.await()
                staffList = staffDef.await()
                pendingLeaves = leavesDef.await()
                unreadNotifCount = unreadCountDef.await()
            }

            // Check admin clock in status
            user?.let { u ->
                try {
                    val today = LocalDate.now().toString()
                    val record = com.devcraft.jsmart.supabase.postgrest["attendance"]
                        .select {
                            filter {
                                eq("user_id", u.id)
                                eq("date", today)
                                filter("clock_out_time", FilterOperator.IS, "null")
                            }
                        }.decodeList<AttendanceDbRecord>()
                    isClockedIn = record.isNotEmpty()
                } catch (_: Exception) {}
            }
            isLoading = false
        }
    }

    LaunchedEffect(selectedBranch) {
        refreshData()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            scope.launch {
                isClockLoading = true
                val result = if (isClockedIn) performClockOut(context) else performClockIn(context)
                if (result.success) {
                    isClockedIn = !isClockedIn
                    refreshData()
                }
                snackbarHostState.showSnackbar(result.message)
                isClockLoading = false
            }
        } else {
            scope.launch { snackbarHostState.showSnackbar("Location permission is required to clock in/out") }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Cream
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TealPrimary)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Bar - NO padding above it
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TealPrimary)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("JSmart Admin", color = Cream, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Good morning, $firstName", color = TealLight, fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BadgedBox(
                            badge = {
                                if (unreadNotifCount > 0) {
                                    Badge(containerColor = ErrorRed, contentColor = Color.White) {
                                        Text(unreadNotifCount.toString())
                                    }
                                }
                            }
                        ) {
                            IconButton(onClick = { onNavigate(Routes.NOTIFICATIONS) }) {
                                Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = Cream)
                            }
                        }
                        Box(
                            modifier = Modifier.size(36.dp).background(TealLight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(initial, color = TealPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Scrollable content with padding
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(padding)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        // Branch selector tabs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val kamakisId = "965dc64f-9636-4062-88dc-a5c727f19245"
                            val cbdId = "1ce1489b-8b5b-498e-9d6a-fad2d4000cbe"

                            BranchChip(
                                label = "Bypass Kamakis",
                                selected = selectedBranch == kamakisId,
                                onClick = { selectedBranch = kamakisId }
                            )
                            BranchChip(
                                label = "CBD Branch",
                                selected = selectedBranch == cbdId,
                                onClick = { selectedBranch = cbdId }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Overview cards
                        Text("Today's Overview", fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold, color = CharcoalDark)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatCard("Present Today", "${stats?.presentToday ?: 0}/${stats?.totalStaff ?: 0}", Icons.Filled.CheckCircle,
                                valueColor = SuccessGreen, modifier = Modifier.weight(1f))
                            StatCard("Absent", "${stats?.absentToday ?: 0}", Icons.Filled.Cancel,
                                valueColor = ErrorRed, modifier = Modifier.weight(1f))
                            StatCard("Late Arrivals", "${stats?.lateToday ?: 0}", Icons.Filled.Schedule,
                                valueColor = WarningAmber, modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatCard("Avg Clock-in", stats?.avgClockIn ?: "--", Icons.Filled.AccessTime,
                                modifier = Modifier.weight(1f))
                            StatCard("On Leave", "${stats?.onLeave ?: 0}", Icons.Filled.BeachAccess,
                                modifier = Modifier.weight(1f))
                            StatCard("Monthly Rate", "${stats?.monthlyRate ?: 0}%", Icons.Filled.BarChart,
                                valueColor = TealPrimary, modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Admin Clock In/Out Button
                        Button(
                            onClick = { permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION) },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isClockedIn) ErrorRed else TealPrimary
                            ),
                            enabled = !isClockLoading
                        ) {
                            if (isClockLoading) {
                                CircularProgressIndicator(color = Cream, modifier = Modifier.size(24.dp))
                            } else {
                                Text(if (isClockedIn) "Clock Out" else "Clock In", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Quick Actions
                        Text("Quick Actions", fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold, color = CharcoalDark)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickActionCard("Post\nAnnouncement", Icons.Filled.Campaign,
                                Routes.ANNOUNCEMENTS, onNavigate, Modifier.weight(1f))
                            QuickActionCard("Leave\nApprovals", Icons.Filled.EventAvailable,
                                Routes.LEAVE_APPROVAL, onNavigate, Modifier.weight(1f))
                            QuickActionCard("Assign\nTask", Icons.Filled.AddTask,
                                Routes.TASK_CREATION, onNavigate, Modifier.weight(1f))
                            QuickActionCard("Full\nReports", Icons.Filled.Assessment,
                                Routes.ADMIN_REPORTS, onNavigate, Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Pending leave requests
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Pending Leave Requests", fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold, color = CharcoalDark)
                            TextButton(onClick = { onNavigate(Routes.LEAVE_APPROVAL) }) {
                                Text("See All", color = TealPrimary)
                            }
                        }

                        if (pendingLeaves.isEmpty()) {
                            Text("No pending requests", modifier = Modifier.padding(vertical = 8.dp), color = CharcoalMedium, fontSize = 14.sp)
                        } else {
                            pendingLeaves.take(2).forEach { leave ->
                                LeaveRequestItem(
                                    name = leave.userId.take(8), // Since we don't have a staff name map here yet as requested
                                    type = leave.leaveType,
                                    dates = "${formatDate(leave.startDate)}–${formatDate(leave.endDate)}",
                                    duration = "Request",
                                    onApprove = {
                                        scope.launch {
                                            LeaveRepository.approveLeave(leave.id!!, user?.id ?: "")
                                            refreshData()
                                        }
                                    },
                                    onReject = {
                                        scope.launch {
                                            LeaveRepository.rejectLeave(leave.id!!, user?.id ?: "", "Rejected from dashboard")
                                            refreshData()
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Staff list preview
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Staff Attendance Today", fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold, color = CharcoalDark)
                            TextButton(onClick = { onNavigate(Routes.STAFF_MANAGEMENT) }) {
                                Text("See All", color = TealPrimary)
                            }
                        }

                        staffList.take(5).forEach { row ->
                            val statusText = when (row.status) {
                                "present" -> "Clocked In • ${formatTime(row.clockInTime)}"
                                "late" -> "Late • ${formatTime(row.clockInTime)}"
                                "absent" -> "Absent"
                                "on_leave" -> "On Leave"
                                else -> row.status
                            }
                            val dotColor = when (row.status) {
                                "present", "late" -> SuccessGreen
                                "absent" -> ErrorRed
                                "on_leave" -> WarningAmber
                                else -> CharcoalMedium
                            }

                            StaffAttendanceRow(
                                name = row.fullName,
                                status = statusText,
                                present = row.status == "present" || row.status == "late",
                                statusColor = dotColor,
                                onClick = { onNavigate(Routes.STAFF_DETAIL.replace("{staffId}", row.userId)) }
                            )
                        }

                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun BranchChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) TealPrimary else SurfaceContainer,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (selected) Cream else CharcoalMedium,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

@Composable
fun StatCard(
    label: String, value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    valueColor: Color = CharcoalDark,

) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Icon(icon, contentDescription = null,
                tint = valueColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = valueColor)
            Text(label, fontSize = 10.sp, color = CharcoalMedium)
        }
    }
}

@Composable
fun QuickActionCard(
    label: String, icon: ImageVector,
    route: String, onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onNavigate(route) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TealPrimary)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null,
                tint = Cream, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(label, fontSize = 10.sp, color = Cream,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun LeaveRequestItem(
    name: String, type: String, dates: String, duration: String,
    onApprove: () -> Unit, onReject: () -> Unit
) {
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
                Column {
                    Text(name, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold, color = CharcoalDark)
                    Text("$type • $dates • $duration",
                        fontSize = 12.sp, color = CharcoalMedium)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = WarningAmber.copy(alpha = 0.15f)
                ) {
                    Text("Pending", color = WarningAmber, fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                ) { Text("Reject", fontSize = 13.sp) }
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) { Text("Approve", fontSize = 13.sp) }
            }
        }
    }
}

@Composable
fun StaffAttendanceRow(
    name: String,
    status: String,
    present: Boolean,
    statusColor: Color = if (present) SuccessGreen else ErrorRed,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp)
                .background(SurfaceContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(name.first().toString(), color = TealPrimary,
                fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontSize = 14.sp,
                fontWeight = FontWeight.Medium, color = CharcoalDark)
            Text(status, fontSize = 12.sp,
                color = if (present) CharcoalMedium else statusColor)
        }
        Box(
            modifier = Modifier.size(10.dp).background(
                statusColor, CircleShape
            )
        )
    }
}