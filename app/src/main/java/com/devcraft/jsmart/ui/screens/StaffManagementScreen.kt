@file:Suppress("SpellCheckingInspection")
@file:OptIn(ExperimentalMaterial3Api::class)
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devcraft.jsmart.data.StaffRepository
import com.devcraft.jsmart.data.StaffRow
import com.devcraft.jsmart.data.DepartmentRow
import com.devcraft.jsmart.data.BranchBasicInfo
import com.devcraft.jsmart.data.AdminRepository
import com.devcraft.jsmart.AuthRepository
import com.devcraft.jsmart.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun StaffManagementScreen(onNavigate: (String) -> Unit) {
    var allStaff by remember { mutableStateOf<List<StaffRow>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedBranch by remember { mutableStateOf("All") }
    var showAddStaffDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun refreshStaff() {
        scope.launch {
            isLoading = true
            allStaff = StaffRepository.getAllStaff()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshStaff()
    }

    val filteredStaff = allStaff.filter { staff ->
        val matchesBranch = selectedBranch == "All" ||
                staff.branches?.name?.contains(selectedBranch, ignoreCase = true) == true
        val matchesSearch = searchQuery.isEmpty() ||
                staff.fullName.contains(searchQuery, ignoreCase = true) ||
                (staff.email?.contains(searchQuery, ignoreCase = true) == true)
        matchesBranch && matchesSearch
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TealPrimary)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.People, contentDescription = null,
                        tint = Cream, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Staff Management", color = Cream,
                        fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
                IconButton(onClick = { showAddStaffDialog = true }) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = "Add Staff", tint = Cream)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Cream)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Summary cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StaffSummaryCard("Total Staff", allStaff.size.toString(),
                        Icons.Filled.People, modifier = Modifier.weight(1f))
                    StaffSummaryCard("Bypass", allStaff.count { it.branches?.name?.contains("Bypass", ignoreCase = true) == true }.toString(),
                        Icons.Filled.Store, modifier = Modifier.weight(1f))
                    StaffSummaryCard("CBD", allStaff.count { it.branches?.name?.contains("CBD", ignoreCase = true) == true }.toString(),
                        Icons.Filled.LocationCity, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search staff...") },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = TealPrimary)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Branch filter chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Bypass Kamakis", "CBD").forEach { branch ->
                        FilterChip(
                            selected = selectedBranch == branch,
                            onClick = { selectedBranch = branch },
                            label = { Text(branch, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TealPrimary,
                                selectedLabelColor = Cream
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Staff List", fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold, color = CharcoalDark)
                Spacer(modifier = Modifier.height(8.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TealPrimary)
                    }
                } else if (filteredStaff.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("No staff found", color = CharcoalMedium, fontWeight = FontWeight.Medium)
                    }
                } else {
                    filteredStaff.forEach { staff ->
                        StaffMemberCard(
                            staff = staff,
                            onRefresh = {
                                scope.launch {
                                    allStaff = StaffRepository.getAllStaff()
                                }
                            },
                            onShowSnackbar = { message ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(message)
                                }
                            },
                            onClick = { onNavigate("staff_detail/${staff.id}") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showAddStaffDialog) {
        AddStaffDialog(
            onDismiss = { showAddStaffDialog = false },
            onStaffAdded = { fullName, _ ->
                showAddStaffDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Account created for $fullName. Logging out for security. Please log back in.")
                    // AuthRepository.createStaffAccount already logs out
                    onNavigate("login")
                }
            },
            onShowError = { msg ->
                scope.launch {
                    snackbarHostState.showSnackbar(msg)
                }
            }
        )
    }
}

@Composable
fun AddStaffDialog(
    onDismiss: () -> Unit,
    onStaffAdded: (String, String) -> Unit,
    onShowError: (String) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("staff") }
    var branchId by remember { mutableStateOf<String?>(null) }
    var departmentId by remember { mutableStateOf<String?>(null) }

    var branches by remember { mutableStateOf<List<BranchBasicInfo>>(emptyList()) }
    var departments by remember { mutableStateOf<List<DepartmentRow>>(emptyList()) }
    var isSaving by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        branches = AdminRepository.getBranches()
        departments = StaffRepository.getAllDepartments()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Staff") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                // Role Dropdown
                Text("Role", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("staff", "admin").forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                // Branch Dropdown
                var branchMenuExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedTextField(
                        value = branches.find { it.id == branchId }?.name ?: "Select Branch",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Branch") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { branchMenuExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = branchMenuExpanded,
                        onDismissRequest = { branchMenuExpanded = false }
                    ) {
                        branches.forEach { b ->
                            DropdownMenuItem(
                                text = { Text(b.name) },
                                onClick = {
                                    branchId = b.id
                                    branchMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Department Dropdown
                var deptMenuExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedTextField(
                        value = departments.find { it.id == departmentId }?.name ?: "Select Department",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Department") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { deptMenuExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = deptMenuExpanded,
                        onDismissRequest = { deptMenuExpanded = false }
                    ) {
                        departments.forEach { d ->
                            DropdownMenuItem(
                                text = { Text(d.name) },
                                onClick = {
                                    departmentId = d.id
                                    deptMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isBlank() || email.isBlank()) {
                        onShowError("Name and Email are required")
                        return@Button
                    }
                    if (!email.contains("@")) {
                        onShowError("Invalid email")
                        return@Button
                    }
                    isSaving = true
                    scope.launch {
                        val success = AuthRepository.createStaffAccount(
                            fullName = fullName,
                            email = email,
                            phone = phone.ifBlank { null },
                            role = role,
                            branchId = branchId,
                            departmentId = departmentId
                        )
                        isSaving = false
                        if (success) {
                            onStaffAdded(fullName, email)
                        } else {
                            onShowError("Failed to create account")
                        }
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Cream
                    )
                } else {
                    Text("Create Account")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun StaffMemberCard(
    staff: StaffRow,
    onRefresh: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    onClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showBranchDialog by remember { mutableStateOf(false) }
    var showRoleDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(TealPrimary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    staff.fullName.firstOrNull()?.toString() ?: "?",
                    color = TealPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(staff.fullName, fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold, color = CharcoalDark)
                Text(staff.role.replaceFirstChar { it.uppercase() }, fontSize = 12.sp, color = CharcoalMedium)
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Store, contentDescription = null,
                            tint = CharcoalMedium, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(staff.branches?.name ?: "Unassigned", fontSize = 11.sp, color = CharcoalMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Phone, contentDescription = null,
                            tint = CharcoalMedium, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(staff.phone ?: "No phone", fontSize = 11.sp, color = CharcoalMedium)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (staff.isActive)
                        SuccessGreen.copy(alpha = 0.12f)
                    else Color.Gray.copy(alpha = 0.12f)
                ) {
                    Text(
                        if (staff.isActive) "Active" else "Inactive",
                        color = if (staff.isActive) SuccessGreen else Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Options",
                            tint = CharcoalMedium, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Change Branch") },
                            onClick = {
                                showMenu = false
                                showBranchDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Change Role") },
                            onClick = {
                                showMenu = false
                                showRoleDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (staff.isActive) "Deactivate" else "Reactivate") },
                            onClick = {
                                showMenu = false
                                scope.launch {
                                    val success = if (staff.isActive)
                                        StaffRepository.deactivateStaff(staff.id)
                                    else
                                        StaffRepository.reactivateStaff(staff.id)
                                    if (success) {
                                        onShowSnackbar(if (staff.isActive) "Staff deactivated" else "Staff reactivated")
                                        onRefresh()
                                    } else {
                                        onShowSnackbar("Action failed")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showBranchDialog) {
        AlertDialog(
            onDismissRequest = { showBranchDialog = false },
            title = { Text("Select Branch") },
            text = {
                Column {
                    listOf(
                        "Bypass Kamakis" to "965dc64f-9636-4062-88dc-a5c727f19245",
                        "CBD Branch" to "1ce1489b-8b5b-498e-9d6a-fad2d4000cbe"
                    ).forEach { (name, id) ->
                        TextButton(
                            onClick = {
                                showBranchDialog = false
                                scope.launch {
                                    val success = StaffRepository.updateStaffBranch(staff.id, id)
                                    if (success) {
                                        onShowSnackbar("Branch updated")
                                        onRefresh()
                                    } else {
                                        onShowSnackbar("Failed to update branch")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBranchDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showRoleDialog) {
        AlertDialog(
            onDismissRequest = { showRoleDialog = false },
            title = { Text("Select Role") },
            text = {
                Column {
                    listOf("staff", "admin", "super_admin").forEach { r ->
                        TextButton(
                            onClick = {
                                showRoleDialog = false
                                scope.launch {
                                    val success = StaffRepository.updateStaffRole(staff.id, r)
                                    if (success) {
                                        onShowSnackbar("Role updated")
                                        onRefresh()
                                    } else {
                                        onShowSnackbar("Failed to update role")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(r.replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoleDialog = false }) { Text("Cancel") }
            }
        )
    }
}


@Composable
fun StaffSummaryCard(
    label: String, value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null,
                tint = TealPrimary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 20.sp,
                fontWeight = FontWeight.Bold, color = TealPrimary)
            Text(label, fontSize = 10.sp, color = CharcoalMedium)
        }
    }
}