@file:Suppress("SpellCheckingInspection")
@file:OptIn(ExperimentalMaterial3Api::class)
package com.devcraft.jsmart.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devcraft.jsmart.UserSession
import com.devcraft.jsmart.data.AttendanceDbRecord
import com.devcraft.jsmart.data.AttendanceRepository
import com.devcraft.jsmart.data.AnnouncementRepository
import com.devcraft.jsmart.data.AnnouncementRow
import com.devcraft.jsmart.data.AnnouncementSession
import com.devcraft.jsmart.data.NotificationRepository
import com.devcraft.jsmart.navigation.Routes
import com.devcraft.jsmart.supabase
import com.devcraft.jsmart.ui.theme.*
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.launch

@Composable
fun EmployeeHomeScreen(onNavigate: (String) -> Unit) {
    var isClockedIn by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var wifiOk by remember { mutableStateOf(false) }
    var hoursToday by remember { mutableStateOf("0h 00m") }
    var clockInTime by remember { mutableStateOf<String?>(null) }
    var recentRecords by remember { mutableStateOf<List<AttendanceDbRecord>>(emptyList()) }
    var recentAnnouncements by remember {
        mutableStateOf<List<AnnouncementRow>>(emptyList())
    }
    var unreadNotifCount by remember { mutableIntStateOf(0) }
    var attendanceRate by remember { mutableStateOf("0%") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val user = UserSession.get()
    val firstName = user?.fullName?.split(" ")?.firstOrNull()
        ?.replaceFirstChar { it.uppercase() } ?: "there"
    val branch = user?.displayBranch ?: "JSmart"

    // On load: check if already clocked in today + load recent records
    val userId = user?.id ?: ""
    LaunchedEffect(userId) {
        if (userId.isEmpty()) {
            isLoading = false
            return@LaunchedEffect
        }
        scope.launch {
            try {
                val today = java.time.LocalDate.now().toString()

                // Only look for an OPEN record — clocked in, not yet clocked out
                val openRecord = supabase.postgrest["attendance"]
                    .select {
                        filter {
                            eq("user_id", userId)
                            eq("date", today)
                            filter("clock_out_time", FilterOperator.IS, "null")
                        }
                        limit(1)
                    }
                    .decodeList<AttendanceDbRecord>()

                if (openRecord.isNotEmpty()) {
                    isClockedIn = true
                    clockInTime = openRecord.first().clockInTime
                } else {
                    isClockedIn = false
                    clockInTime = null
                    hoursToday = "0h 00m"
                }

                // Load recent activity
                recentRecords = AttendanceRepository.getMyAttendanceRecords().take(3)

                // Load announcements
                val allAnnouncements = AnnouncementRepository.getAnnouncements()
                recentAnnouncements = allAnnouncements.take(3)

                // Load unread count
                unreadNotifCount = NotificationRepository.getUnreadCount(userId)

                // FIX 1 — Attendance % calculation
                val allMonthRecords = AttendanceRepository.getMyAttendanceRecords()
                val monthPresent = allMonthRecords.count {
                    it.status.lowercase() == "present" || it.status.lowercase() == "late"
                }
                val monthTotal = allMonthRecords.size
                val rate = if (monthTotal > 0) (monthPresent * 100 / monthTotal) else 0
                attendanceRate = "${rate}%"

            } catch (e: Exception) {
                android.util.Log.e("HomeScreen", "Load error: ${e.message}")
            }
            isLoading = false
        }
    }

    // Live hours counter — ticks every minute while clocked in
    LaunchedEffect(isClockedIn, clockInTime) {
        if (isClockedIn && clockInTime != null) {
            while (true) {
                hoursToday = calculateHours(clockInTime, java.time.Instant.now().toString())
                kotlinx.coroutines.delay(60_000) // update every minute
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                isLoading = true
                val result = if (isClockedIn) {
                    performClockOut(context)
                } else {
                    performClockIn(context)
                }
                if (result.success) {
                    isClockedIn = !isClockedIn
                    if (!isClockedIn) {
                        // just clocked out
                        val savedClockIn = clockInTime
                        hoursToday = calculateHours(savedClockIn, java.time.Instant.now().toString())
                        clockInTime = null
                    } else {
                        // just clocked in
                        clockInTime = java.time.Instant.now().toString()
                    }
                    wifiOk = result.wifiVerified
                    // Refresh records after clock action
                    recentRecords = AttendanceRepository.getMyAttendanceRecords().take(3)
                }
                isLoading = false
                snackbarHostState.showSnackbar(result.message)
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Location permission required to clock in")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Cream)
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TealPrimary)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("JSmart", color = Cream, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(branch, color = TealLight, fontSize = 11.sp)
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
                        modifier = Modifier
                            .size(36.dp)
                            .background(TealLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            user?.displayInitial ?: "?",
                            color = TealPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Main Content
            Column(modifier = Modifier.padding(16.dp)) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Good morning, $firstName",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CharcoalDark
                )
                Text(
                    "Ready for your shift at $branch?",
                    fontSize = 14.sp,
                    color = CharcoalMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Geofence Map Placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(TealLight.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(TealLight.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = TealPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Map View", color = CharcoalMedium, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Geofence / Clock Status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isClockedIn) SuccessGreen.copy(alpha = 0.1f) else SurfaceContainer,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isClockedIn) Icons.Filled.CheckCircle else Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = if (isClockedIn) SuccessGreen else CharcoalMedium,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isClockedIn)
                            "Clocked in ✓${if (wifiOk) " • On JSmart WiFi" else ""}"
                        else
                            "Tap Clock In to verify your location",
                        color = if (isClockedIn) SuccessGreen else CharcoalMedium,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Clock In/Out Button
                Button(
                    onClick = {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isClockedIn) ErrorRed else TealPrimary
                    ),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Cream,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Filled.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isClockedIn) "Clock Out" else "Clock In",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Summary Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        label = "Hours Today",
                        value = hoursToday
                    )
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        label = "Status",
                        value = if (isClockedIn) "Present" else "Not In",
                        valueColor = if (isClockedIn) SuccessGreen else CharcoalMedium
                    )
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        label = "This Week",
                        value = attendanceRate
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Announcements
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Announcements",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CharcoalDark
                    )
                    TextButton(onClick = { onNavigate(Routes.ANNOUNCEMENTS) }) {
                        Text("View All", color = TealPrimary)
                    }
                }

                if (recentAnnouncements.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No announcements yet",
                            color = CharcoalMedium,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                } else {
                    recentAnnouncements.forEach { announcement ->
                        AnnouncementCard(
                            tag = announcement.priority ?: "Update",
                            title = announcement.title,
                            preview = announcement.body,
                            onClick = {
                                AnnouncementSession.current = announcement
                                onNavigate(Routes.ANNOUNCEMENT_DETAIL)
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Recent Activity — real data
                Text(
                    "Recent Activity",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CharcoalDark
                )
                Spacer(modifier = Modifier.height(8.dp))

                 if (recentRecords.isEmpty() && !isLoading) {
                     Text(
                         "No recent activity",
                         fontSize = 13.sp,
                         color = CharcoalMedium,
                         modifier = Modifier.padding(vertical = 8.dp)
                     )
                 } else {
                     recentRecords.forEach { record ->
                          if (record.clockInTime != null) {
                              ActivityItem(
                                  icon = Icons.AutoMirrored.Filled.Login,
                                  title = "Clocked In",
                                  subtitle = "${formatTime(record.clockInTime)} • ${record.status.replaceFirstChar { it.uppercase() }}",
                                  trailing = "",
                                  onClick = { onNavigate(Routes.ATTENDANCE_HISTORY) }
                              )
                          }

                          if (record.clockOutTime != null) {
                              val hours = calculateHours(record.clockInTime, record.clockOutTime)
                              ActivityItem(
                                  icon = Icons.AutoMirrored.Filled.Logout,
                                  title = "Clocked Out",
                                  subtitle = formatTime(record.clockOutTime),
                                  trailing = hours,
                                  onClick = { onNavigate(Routes.ATTENDANCE_HISTORY) }
                              )
                          }
                      }
                  }

                 Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}


@Composable
fun SummaryCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color = CharcoalDark
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, fontSize = 10.sp, color = CharcoalMedium, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

@Composable
fun AnnouncementCard(tag: String, title: String, preview: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(tag, fontSize = 10.sp, color = TealPrimary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CharcoalDark)
            Spacer(modifier = Modifier.height(4.dp))
            Text(preview, fontSize = 12.sp, color = CharcoalMedium)
        }
    }
}

@Composable
fun ActivityItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: String,
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
            modifier = Modifier
                .size(40.dp)
                .background(SurfaceContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = TealPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = CharcoalDark)
            Text(subtitle, fontSize = 12.sp, color = CharcoalMedium)
        }
        Text(trailing, fontSize = 12.sp, color = CharcoalMedium)
    }
}