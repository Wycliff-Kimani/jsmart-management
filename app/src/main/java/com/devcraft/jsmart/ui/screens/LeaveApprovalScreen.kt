@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.devcraft.jsmart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.devcraft.jsmart.UserSession
import com.devcraft.jsmart.data.LeaveRepository
import com.devcraft.jsmart.data.LeaveRow
import com.devcraft.jsmart.ui.theme.*
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.devcraft.jsmart.supabase

@Serializable
data class UserNameRow(
    val id: String,
    @SerialName("full_name") val fullName: String
)

@Composable
fun LeaveApprovalScreen(onBack: () -> Unit = {}) {
    val reviewerId = UserSession.get()?.id ?: ""
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Pending", "Approved", "Rejected")

    var pendingRequests by remember { mutableStateOf<List<LeaveRow>>(emptyList()) }
    var approvedRequests by remember { mutableStateOf<List<LeaveRow>>(emptyList()) }
    var rejectedRequests by remember { mutableStateOf<List<LeaveRow>>(emptyList()) }
    var staffNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun formatLeaveDate(raw: String?): String {
        if (raw.isNullOrBlank()) return "--"
        return try {
            val parts = raw.trim().take(10).split("-")
            val months = listOf("","Jan","Feb","Mar","Apr","May",
                "Jun","Jul","Aug","Sep","Oct","Nov","Dec")
            val month = months[parts[1].toInt()]
            val day = parts[2].toInt()
            val year = parts[0]
            "$month $day, $year"
        } catch (e: Exception) { raw }
    }

    var showRejectDialog by remember { mutableStateOf<String?>(null) } // holds leaveId
    var rejectionReason by remember { mutableStateOf("") }

    fun refreshData() {
        scope.launch {
            isLoading = true
            try {
                val all = LeaveRepository.getAllRequests()
                pendingRequests = all.filter { it.status == "pending" }
                approvedRequests = all.filter { it.status == "approved" }
                rejectedRequests = all.filter { it.status == "rejected" }

                val users = supabase.postgrest["users"]
                    .select(Columns.raw("id, full_name"))
                    .decodeList<UserNameRow>()
                staffNames = users.associate { it.id to it.fullName }
            } catch (e: Exception) {
                // error handling
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    if (showRejectDialog != null) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = null },
            title = { Text("Reject Leave Request") },
            text = {
                Column {
                    Text("Enter the reason for rejection:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Reason...") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val leaveId = showRejectDialog!!
                    scope.launch {
                        try {
                            LeaveRepository.rejectLeave(leaveId, reviewerId, rejectionReason)
                            snackbarHostState.showSnackbar("Leave rejected")
                            refreshData()
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Failed to reject")
                        } finally {
                            showRejectDialog = null
                            rejectionReason = ""
                        }
                    }
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRejectDialog = null
                    rejectionReason = ""
                }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Cream)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TealPrimary)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Cream)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Filled.BeachAccess, contentDescription = null,
                    tint = Cream, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Leave Approvals", color = Cream,
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

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TealPrimary)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    // Summary row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LeaveApprovalStatCard("Pending", pendingRequests.size.toString(),
                            WarningAmber, modifier = Modifier.weight(1f))
                        LeaveApprovalStatCard("Approved", approvedRequests.size.toString(),
                            SuccessGreen, modifier = Modifier.weight(1f))
                        LeaveApprovalStatCard("Rejected", rejectedRequests.size.toString(),
                            ErrorRed, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    when (selectedTab) {
                        0 -> PendingLeaveList(pendingRequests, staffNames, onApprove = { leaveId ->
                            scope.launch {
                                try {
                                    LeaveRepository.approveLeave(leaveId, reviewerId)
                                    snackbarHostState.showSnackbar("Leave approved")
                                    refreshData()
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Failed to approve")
                                }
                            }
                        }, onReject = { leaveId ->
                            showRejectDialog = leaveId
                        }, formatLeaveDate = ::formatLeaveDate)
                        1 -> ApprovedLeaveList(approvedRequests, staffNames, formatLeaveDate = ::formatLeaveDate)
                        2 -> RejectedLeaveList(rejectedRequests, staffNames, formatLeaveDate = ::formatLeaveDate)
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun PendingLeaveList(
    requests: List<LeaveRow>,
    staffNames: Map<String, String>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    formatLeaveDate: (String?) -> String
) {
    Text("Awaiting Your Approval", fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold, color = CharcoalDark)
    Spacer(modifier = Modifier.height(8.dp))

    requests.forEach { request ->
        LeaveRequestItem(
            name = staffNames[request.userId] ?: request.userId.take(8),
            type = request.leaveType,
            dates = "${formatLeaveDate(request.startDate)}—${formatLeaveDate(request.endDate)}",
            duration = "", // Calculate if needed, but keeping UI simple for now
            reason = request.reason ?: "",
            onApprove = { onApprove(request.id!!) },
            onReject = { onReject(request.id!!) }
        )
    }
}

@Composable
fun ApprovedLeaveList(
    requests: List<LeaveRow>,
    staffNames: Map<String, String>,
    formatLeaveDate: (String?) -> String
) {
    Text("Approved Requests", fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold, color = CharcoalDark)
    Spacer(modifier = Modifier.height(8.dp))

    requests.forEach { request ->
        val item = LeaveApprovalItem(
            name = staffNames[request.userId] ?: request.userId.take(8),
            type = request.leaveType,
            dates = "${formatLeaveDate(request.startDate)}—${formatLeaveDate(request.endDate)}",
            duration = "",
            reason = request.reason ?: ""
        )
        LeaveStatusCard(item, "Approved", SuccessGreen)
    }
}

@Composable
fun RejectedLeaveList(
    requests: List<LeaveRow>,
    staffNames: Map<String, String>,
    formatLeaveDate: (String?) -> String
) {
    Text("Rejected Requests", fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold, color = CharcoalDark)
    Spacer(modifier = Modifier.height(8.dp))

    requests.forEach { request ->
        val item = LeaveApprovalItem(
            name = staffNames[request.userId] ?: request.userId.take(8),
            type = request.leaveType,
            dates = "${formatLeaveDate(request.startDate)}—${formatLeaveDate(request.endDate)}",
            duration = "",
            reason = request.reason ?: ""
        )
        LeaveStatusCard(item, "Rejected", ErrorRed)
    }
}

data class LeaveApprovalItem(
    val name: String, val type: String,
    val dates: String, val duration: String,
    val reason: String
)

@Composable
fun LeaveRequestItem(
    name: String, type: String, dates: String,
    duration: String, reason: String = "",
    onApprove: () -> Unit, onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(TealPrimary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(name.first().toString(), color = TealPrimary,
                        fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold, color = CharcoalDark)
                    Text(type, fontSize = 12.sp, color = TealPrimary)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = WarningAmber.copy(alpha = 0.12f)
                ) {
                    Text("Pending", color = WarningAmber,
                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null,
                        tint = CharcoalMedium, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(dates, fontSize = 12.sp, color = CharcoalMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Schedule, contentDescription = null,
                        tint = CharcoalMedium, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(duration, fontSize = 12.sp, color = CharcoalMedium)
                }
            }

            if (reason.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Reason: $reason", fontSize = 12.sp,
                    color = CharcoalMedium, fontWeight = FontWeight.Normal)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null,
                        modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject", fontSize = 13.sp)
                }
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null,
                        tint = Cream, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Approve", color = Cream, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun LeaveStatusCard(item: LeaveApprovalItem, status: String, color: androidx.compose.ui.graphics.Color) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(TealPrimary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(item.name.first().toString(), color = TealPrimary,
                    fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold, color = CharcoalDark)
                Text("${item.type} • ${item.dates} • ${item.duration}",
                    fontSize = 12.sp, color = CharcoalMedium)
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = color.copy(alpha = 0.12f)
            ) {
                Text(status, color = color,
                    fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
            }
        }
    }
}

@Composable
fun LeaveApprovalStatCard(
    label: String, value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 22.sp,
                fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 11.sp, color = CharcoalMedium)
        }
    }
}