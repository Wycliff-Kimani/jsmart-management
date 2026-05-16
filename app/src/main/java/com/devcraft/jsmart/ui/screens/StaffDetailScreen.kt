@file:Suppress("SpellCheckingInspection")
@file:OptIn(ExperimentalMaterial3Api::class)
package com.devcraft.jsmart.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devcraft.jsmart.data.*
import com.devcraft.jsmart.supabase
import com.devcraft.jsmart.ui.theme.*
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun StaffDetailScreen(
    staffId: String,
    onBack: () -> Unit
) {
    var staffData by remember { mutableStateOf<StaffRow?>(null) }
    var attendanceRecords by remember { mutableStateOf<List<AttendanceDbRecord>>(emptyList()) }
    var leaveRecords by remember { mutableStateOf<List<LeaveRow>>(emptyList()) }
    var taskRecords by remember { mutableStateOf<List<TaskRow>>(emptyList()) }
    var leaveBalance by remember { mutableStateOf(LeaveBalance()) } // Added state
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun loadData() {
        scope.launch {
            isLoading = true
            val deferredStaff = async { StaffRepository.getStaffById(staffId) }
            val deferredAttendance = async {
                try {
                    supabase.postgrest["attendance"]
                        .select {
                            filter { eq("user_id", staffId) }
                            order("date", Order.DESCENDING)
                            limit(30)
                        }
                        .decodeList<AttendanceDbRecord>()
                } catch (_: Exception) {
                    emptyList<AttendanceDbRecord>()
                }
            }
            val deferredLeave = async { LeaveRepository.getLeaveForStaff(staffId) }
            val deferredTasks = async { TaskRepository.getTasksForStaff(staffId) }
            val deferredBalance = async { LeaveRepository.getLeaveBalance(staffId) } // Added async call

            staffData = deferredStaff.await()
            attendanceRecords = deferredAttendance.await()
            leaveRecords = deferredLeave.await()
            taskRecords = deferredTasks.await()
            leaveBalance = deferredBalance.await() // Wait for balance
            isLoading = false
        }
    }

    LaunchedEffect(staffId) {
        loadData()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(staffData?.fullName ?: "Staff Detail", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TealPrimary,
                    titleContentColor = Cream,
                    navigationIconContentColor = Cream
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TealPrimary)
            }
        } else if (staffData == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Staff member not found")
            }
        } else {
            val staff = staffData!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Cream)
            ) {
                // Profile Header Card
                ProfileHeaderCard(staff)

                // Tabs
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Cream,
                    contentColor = TealPrimary,
                    indicator = {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(selectedTab),
                            color = TealPrimary
                        )
                    }
                ) {
                    val tabs = listOf("Attendance", "Leave", "Tasks", "Manage")
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                        )
                    }
                }

                // Tab Content
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (selectedTab) {
                        0 -> AttendanceTab(attendanceRecords)
                        1 -> LeaveTab(leaveRecords, leaveBalance) // Pass balance
                        2 -> TasksTab(taskRecords)
                        3 -> ManageTab(staff, onUpdate = { loadData() }, onShowMessage = { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileHeaderCard(staff: StaffRow) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(TealPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    staff.fullName.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase(),
                    color = Cream,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(staff.fullName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CharcoalDark)
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (staff.isActive) SuccessGreen.copy(alpha = 0.1f) else ErrorRed.copy(alpha = 0.1f)
                    ) {
                        Text(
                            if (staff.isActive) "Active" else "Inactive",
                            color = if (staff.isActive) SuccessGreen else ErrorRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text("${staff.role.replaceFirstChar { it.uppercase() }} • ${staff.branches?.name ?: "No Branch"}", fontSize = 13.sp, color = CharcoalMedium)
                Text("Emp ID: ${staff.employeeNumber ?: "N/A"}", fontSize = 13.sp, color = CharcoalMedium)

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = CharcoalMedium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(staff.email ?: "No email", fontSize = 12.sp, color = CharcoalMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = CharcoalMedium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(staff.phone ?: "No phone", fontSize = 12.sp, color = CharcoalMedium)
                }
            }
        }
    }
}


@Composable
fun AttendanceTab(records: List<AttendanceDbRecord>) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        // Simple Month Summary Card (Hardcoded logic based on records for now as per instructions style)
        val presentCount = records.count { it.status == "present" }
        val lateCount = records.count { it.status == "late" }
        val absentCount = records.count { it.status == "absent" }
        val total = presentCount + lateCount + absentCount
        val rate = if (total > 0) ((presentCount + lateCount) * 100 / total) else 0

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = TealPrimary)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy")), color = TealLight, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MonthlyStat("Attendance", "$rate%")
                    MonthlyStat("Present", "$presentCount")
                    MonthlyStat("Late", "$lateCount")
                    MonthlyStat("Absent", "$absentCount")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Recent Records", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CharcoalDark)
        Spacer(modifier = Modifier.height(8.dp))

        if (records.isEmpty()) {
            Text("No attendance records", modifier = Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center, color = CharcoalMedium)
        } else {
            records.forEach { AttendanceRecordRow(it) }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun LeaveTab(records: List<LeaveRow>, balance: LeaveBalance) { // Updated signature
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        // Leave Balance Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
        ) {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                BalanceItem("Annual", "${balance.annualRemaining}/${balance.annualTotal}")
                BalanceItem("Sick", "${balance.sickRemaining}/${balance.sickTotal}")
                BalanceItem("Used", "${balance.annualUsed + balance.sickUsed}")
                BalanceItem("Left", "${balance.annualRemaining + balance.sickRemaining}")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Leave Requests", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CharcoalDark)
        Spacer(modifier = Modifier.height(8.dp))

        if (records.isEmpty()) {
            Text("No leave requests", modifier = Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center, color = CharcoalMedium)
        } else {
            records.forEach { leave ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(leave.leaveType.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${leave.startDate} to ${leave.endDate}", fontSize = 12.sp, color = CharcoalMedium)
                        }
                        StatusBadge(leave.status.replaceFirstChar { it.uppercase() })
                    }
                }
            }
        }
    }
}

@Composable
fun BalanceItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
        Text(label, fontSize = 10.sp, color = CharcoalMedium)
    }
}

@Composable
fun TasksTab(records: List<TaskRow>) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        if (records.isEmpty()) {
            Text("No tasks assigned", modifier = Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center, color = CharcoalMedium)
        } else {
            records.forEach { task ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(task.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when(task.priority.lowercase()) {
                                    "high" -> ErrorRed.copy(alpha = 0.1f)
                                    "low" -> SuccessGreen.copy(alpha = 0.1f)
                                    else -> WarningAmber.copy(alpha = 0.1f)
                                }
                            ) {
                                Text(
                                    task.priority.uppercase(),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when(task.priority.lowercase()) {
                                        "high" -> ErrorRed
                                        "low" -> SuccessGreen
                                        else -> WarningAmber
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusBadge(task.status.replace("_", " ").replaceFirstChar { it.uppercase() })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Due: ${task.dueDate ?: "N/A"}", fontSize = 12.sp, color = CharcoalMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ManageTab(staff: StaffRow, onUpdate: () -> Unit, onShowMessage: (String) -> Unit) {
    var showBranchDialog by remember { mutableStateOf(false) }
    var showRoleDialog by remember { mutableStateOf(false) }
    var showDeactivateConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        ManageSection(title = "Branch Assignment", currentValue = staff.branches?.name ?: "Unassigned") {
            Button(
                onClick = { showBranchDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Change Branch", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ManageSection(title = "Role", currentValue = staff.role.replaceFirstChar { it.uppercase() }) {
            Button(
                onClick = { showRoleDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Change Role", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = CharcoalMedium.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(24.dp))

        Text("Account Status", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CharcoalDark)
        Spacer(modifier = Modifier.height(12.dp))

        if (staff.isActive) {
            OutlinedButton(
                onClick = { showDeactivateConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                border = BorderStroke(1.dp, ErrorRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.PersonOff, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Deactivate Staff")
            }
        } else {
            Button(
                onClick = {
                    scope.launch {
                        val success = StaffRepository.reactivateStaff(staff.id)
                        if (success) {
                            onShowMessage("Staff reactivated")
                            onUpdate()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reactivate Staff")
            }
        }
    }

    if (showBranchDialog) {
        AlertDialog(
            onDismissRequest = {
                showBranchDialog = false
            },
            title = { Text("Select Branch") },
            text = {
                Column {
                    listOf(
                        "Bypass Kamakis" to "965dc64f-9636-4062-88dc-a5c727f19245",
                        "CBD Branch" to "1ce1489b-8b5b-498e-9d6a-fad2d4000cbe"
                    ).forEach { (name, id) ->
                        TextButton(onClick = {
                            scope.launch {
                                if (StaffRepository.updateStaffBranch(staff.id, id)) {
                                    onShowMessage("Branch updated")
                                    onUpdate()
                                }
                            }
                            showBranchDialog = false
                        }, modifier = Modifier.fillMaxWidth()) { Text(name) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showBranchDialog = false
                }) { Text("Cancel") }
            }
        )
    }

    if (showRoleDialog) {
        AlertDialog(
            onDismissRequest = {
                showRoleDialog = false
            },
            title = { Text("Select Role") },
            text = {
                Column {
                    listOf("staff", "admin", "super_admin").forEach { role ->
                        TextButton(onClick = {
                            scope.launch {
                                if (StaffRepository.updateStaffRole(staff.id, role)) {
                                    onShowMessage("Role updated")
                                    onUpdate()
                                }
                            }
                            showRoleDialog = false
                        }, modifier = Modifier.fillMaxWidth()) { Text(role.replaceFirstChar { it.uppercase() }) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showRoleDialog = false
                }) { Text("Cancel") }
            }
        )
    }

    if (showDeactivateConfirm) {
        AlertDialog(
            onDismissRequest = {
                showDeactivateConfirm = false
            },
            title = { Text("Deactivate Staff") },
            text = { Text("Are you sure you want to deactivate ${staff.fullName}? They will no longer be able to log in.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        if (StaffRepository.deactivateStaff(staff.id)) {
                            onShowMessage("Staff deactivated")
                            onUpdate()
                        }
                    }
                    showDeactivateConfirm = false
                }, colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)) { Text("Deactivate") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeactivateConfirm = false
                }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ManageSection(title: String, currentValue: String, content: @Composable () -> Unit) {
    Column {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CharcoalDark)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(currentValue, fontSize = 14.sp, color = CharcoalMedium)
            content()
        }
    }
}
