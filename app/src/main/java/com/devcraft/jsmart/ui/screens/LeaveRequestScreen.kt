@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.devcraft.jsmart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.devcraft.jsmart.UserSession
import com.devcraft.jsmart.data.LeaveRepository
import com.devcraft.jsmart.data.LeaveRow
import com.devcraft.jsmart.ui.theme.*
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@Composable
fun LeaveRequestScreen(isAdmin: Boolean = false) {
    val userId = UserSession.get()?.id ?: ""
    var selectedTab by remember { mutableIntStateOf(0) }

    var myRequests by remember { mutableStateOf<List<LeaveRow>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            isLoading = true
            myRequests = LeaveRepository.getMyLeaveRequests(userId)
            isLoading = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Cream)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TealPrimary)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.BeachAccess, contentDescription = null,
                    tint = Cream, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Leave", color = Cream,
                    fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }

            // Tabs
            val tabs = if (isAdmin)
                listOf("My Requests", "Pending Approvals")
            else
                listOf("My Requests", "Request Leave")

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
                        .padding(16.dp)
                ) {
                    when (selectedTab) {
                        0 -> MyLeaveRequestsTab(myRequests)
                        1 -> if (isAdmin) PendingApprovalsTab()
                        else RequestLeaveTab(userId, onSubmitted = {
                            scope.launch {
                                myRequests = LeaveRepository.getMyLeaveRequests(userId)
                                snackbarHostState.showSnackbar("Leave request submitted")
                            }
                        }, snackbarHostState = snackbarHostState)
                    }
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun MyLeaveRequestsTab(myRequests: List<LeaveRow>) {
    // Summary card
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
            LeaveBalanceStat("Annual", "12 days")
            LeaveBalanceStat("Sick", "7 days")
            LeaveBalanceStat("Used", "5 days")
            LeaveBalanceStat("Remaining", "14 days")
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text("My Leave History", fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold, color = CharcoalDark)
    Spacer(modifier = Modifier.height(8.dp))

    if (myRequests.isEmpty()) {
        Text("No leave requests found", fontSize = 14.sp, color = CharcoalMedium)
    } else {
        myRequests.forEach { item ->
            LeaveHistoryCard(item)
        }
    }
}

@Composable
fun RequestLeaveTab(
    userId: String,
    onSubmitted: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var leaveType by remember { mutableStateOf("Annual Leave") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val today = java.time.LocalDate.now()
    val todayMillis = today
        .atStartOfDay(java.time.ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()

    val startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = todayMillis)
    val endDatePickerState = rememberDatePickerState(initialSelectedDateMillis = todayMillis)

    val displayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val storageFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    val scope = rememberCoroutineScope()

    val leaveTypes = listOf(
        "Annual Leave",
        "Sick Leave",
        "Emergency Leave",
        "Compassionate Leave",
        "Maternity Leave",
        "Paternity Leave",
        "Unpaid Leave"
    )

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDatePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC)
                            .toLocalDate()
                        startDate = date.format(storageFormatter)
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = startDatePickerState)
        }
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDatePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC)
                            .toLocalDate()
                        endDate = date.format(storageFormatter)
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = endDatePickerState)
        }
    }

    Text("New Leave Request", fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold, color = CharcoalDark)
    Spacer(modifier = Modifier.height(12.dp))

    // Leave type dropdown
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = leaveType,
            onValueChange = {},
            readOnly = true,
            label = { Text("Leave Type") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            leaveTypes.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type) },
                    onClick = {
                        leaveType = type
                        expanded = false
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = if (startDate.isNotEmpty()) java.time.LocalDate.parse(startDate).format(displayFormatter) else "",
                onValueChange = { },
                readOnly = true,
                label = { Text("Start Date") },
                placeholder = { Text("DD/MM/YYYY") },
                leadingIcon = {
                    Icon(Icons.Filled.CalendarMonth,
                        contentDescription = null, tint = TealPrimary)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showStartDatePicker = true }
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = if (endDate.isNotEmpty()) java.time.LocalDate.parse(endDate).format(displayFormatter) else "",
                onValueChange = { },
                readOnly = true,
                label = { Text("End Date") },
                placeholder = { Text("DD/MM/YYYY") },
                leadingIcon = {
                    Icon(Icons.Filled.CalendarMonth,
                        contentDescription = null, tint = TealPrimary)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showEndDatePicker = true }
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = reason,
        onValueChange = { reason = it },
        label = { Text("Reason (optional)") },
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(12.dp),
        maxLines = 5
    )

    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = {
            if (startDate.isBlank() || endDate.isBlank()) {
                scope.launch { snackbarHostState.showSnackbar("Please fill dates") }
                return@Button
            }
            isSubmitting = true
            scope.launch {
                try {
                    LeaveRepository.submitLeaveRequest(
                        LeaveRow(
                            userId = userId,
                            leaveType = leaveType,
                            startDate = startDate,
                            endDate = endDate,
                            reason = reason,
                            status = "pending"
                        )
                    )
                    startDate = ""
                    endDate = ""
                    reason = ""
                    onSubmitted()
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to submit request")
                } finally {
                    isSubmitting = false
                }
            }
        },
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
        enabled = !isSubmitting
    ) {
        if (isSubmitting) {
            CircularProgressIndicator(color = Cream, modifier = Modifier.size(24.dp))
        } else {
            Icon(Icons.Filled.Send, contentDescription = null, tint = Cream)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Submit Request", color = Cream,
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun PendingApprovalsTab() {
    Text("Pending Requests", fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold, color = CharcoalDark)
    Spacer(modifier = Modifier.height(8.dp))

    listOf(
        Triple("Brian Omondi", "Annual Leave • May 15–18 • 4 days", ""),
        Triple("Aisha Mwangi", "Sick Leave • May 12–13 • 2 days", ""),
        Triple("Cynthia Njeri", "Emergency Leave • May 20 • 1 day", ""),
    ).forEach { (name, detail, _) ->
        LeaveRequestItem(
            name = name,
            type = detail.substringBefore(" •"),
            dates = detail.substringAfter("• ").substringBefore(" •"),
            duration = detail.substringAfterLast("• "),
            onApprove = {},
            onReject = {}
        )
    }
}

data class LeaveItem(val type: String, val dates: String,
                     val duration: String, val status: String)

@Composable
fun LeaveHistoryCard(item: LeaveRow) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.leaveType, fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold, color = CharcoalDark)
                Text("${item.startDate} – ${item.endDate}",
                    fontSize = 12.sp, color = CharcoalMedium)
            }
            StatusBadge(item.status.replaceFirstChar { it.uppercase() })
        }
    }
}

// StatusBadge is defined centrally in AttendanceHistoryScreen to avoid duplicate declarations

@Composable
fun LeaveBalanceStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Cream,
            fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TealLight, fontSize = 10.sp)
    }
}