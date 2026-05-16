@file:Suppress("SpellCheckingInspection")
package com.devcraft.jsmart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import com.devcraft.jsmart.UserSession
import com.devcraft.jsmart.data.TaskRepository
import com.devcraft.jsmart.data.TaskRow
import com.devcraft.jsmart.supabase
import com.devcraft.jsmart.ui.theme.*
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*
import java.text.SimpleDateFormat

@Serializable
data class UserBrief(
    val id: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("branch_id") val branchId: String? = null
)

@Serializable
data class BranchBrief(val id: String, val name: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCreationScreen(onBack: () -> Unit = {}) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var assignedToName by remember { mutableStateOf("") }
    var selectedStaffId by remember { mutableStateOf<String?>(null) }

    // Date Picker state
    var showDueDatePicker by remember { mutableStateOf(false) }
    var dueDateDisplay by remember { mutableStateOf("") }
    var dueDateForDb by remember { mutableStateOf<String?>(null) }

    var priority by remember { mutableStateOf("Medium") }

    var selectedBranchName by remember { mutableStateOf("All Branches") }
    var selectedBranchId by remember { mutableStateOf<String?>(null) }

    var staffExpanded by remember { mutableStateOf(false) }
    var branchExpanded by remember { mutableStateOf(false) }

    var staffList by remember { mutableStateOf<List<UserBrief>>(emptyList()) }
    var branchList by remember { mutableStateOf<List<BranchBrief>>(emptyList()) }
    var isSaving by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val filteredStaff = if (selectedBranchId == null)
        staffList
    else
        staffList.filter { it.branchId == selectedBranchId }

    LaunchedEffect(Unit) {
        try {
            // Load Staff
            val users = supabase.postgrest["users"]
                .select {
                    filter { eq("is_active", true) }
                }
                .decodeList<UserBrief>()
            staffList = users

            // Load Branches
            val branches = supabase.postgrest["branches"]
                .select()
                .decodeList<BranchBrief>()
            branchList = branches
        } catch (_: Exception) {
            scope.launch {
                snackbarHostState.showSnackbar("Failed to load staff/branches")
            }
        }
    }

    if (showDueDatePicker) {
        val todayMillis = java.time.LocalDate.now()
            .atStartOfDay(java.time.ZoneOffset.UTC)
            .toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = todayMillis)

        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Date(millis)
                        val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val dbFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        dueDateDisplay = displayFormat.format(date)
                        dueDateForDb = dbFormat.format(date)
                    }
                    showDueDatePicker = false
                }) {
                    Text("OK", color = TealPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDueDatePicker = false }) {
                    Text("Cancel", color = CharcoalMedium)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Cream)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("Create Task", color = Cream,
                    fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Cream)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text("Task Details", fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold, color = CharcoalDark)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    placeholder = { Text("e.g. Stock Inventory Check") },
                    leadingIcon = {
                        Icon(Icons.Filled.Task, contentDescription = null, tint = TealPrimary)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("Describe what needs to be done...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Branch selector
                ExposedDropdownMenuBox(
                    expanded = branchExpanded,
                    onExpandedChange = { branchExpanded = !branchExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedBranchName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Branch") },
                        leadingIcon = {
                            Icon(Icons.Filled.Store, contentDescription = null, tint = TealPrimary)
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = branchExpanded)
                        },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = branchExpanded,
                        onDismissRequest = { branchExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Branches") },
                            onClick = {
                                selectedBranchName = "All Branches"
                                selectedBranchId = null
                                branchExpanded = false
                                assignedToName = ""
                                selectedStaffId = null
                            }
                        )
                        branchList.forEach { branch ->
                            DropdownMenuItem(
                                text = { Text(branch.name) },
                                onClick = {
                                    selectedBranchName = branch.name
                                    selectedBranchId = branch.id
                                    branchExpanded = false
                                    assignedToName = ""
                                    selectedStaffId = null
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Assign to staff dropdown
                ExposedDropdownMenuBox(
                    expanded = staffExpanded,
                    onExpandedChange = { staffExpanded = !staffExpanded }
                ) {
                    OutlinedTextField(
                        value = assignedToName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Assign To") },
                        leadingIcon = {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = TealPrimary)
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = staffExpanded)
                        },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = staffExpanded,
                        onDismissRequest = { staffExpanded = false }
                    ) {
                        filteredStaff.forEach { staff ->
                            DropdownMenuItem(
                                text = { Text(staff.fullName) },
                                onClick = {
                                    assignedToName = staff.fullName
                                    selectedStaffId = staff.id
                                    staffExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDueDatePicker = true }
                ) {
                    OutlinedTextField(
                        value = dueDateDisplay,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Due Date") },
                        placeholder = { Text("Tap to select date") },
                        leadingIcon = {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = TealPrimary)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledBorderColor = CharcoalMedium,
                            disabledLabelColor = CharcoalMedium,
                            disabledLeadingIconColor = TealPrimary,
                            disabledTextColor = CharcoalDark,
                            disabledPlaceholderColor = CharcoalMedium
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Priority Level", fontSize = 13.sp,
                    fontWeight = FontWeight.Medium, color = CharcoalDark)
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Low", "Medium", "High").forEach { level ->
                        val isSelected = priority == level
                        val color = when (level) {
                            "High" -> ErrorRed
                            "Medium" -> WarningAmber
                            else -> SuccessGreen
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { priority = level },
                            label = { Text(level, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color,
                                selectedLabelColor = Cream
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = {
                        if (title.isBlank() || selectedStaffId == null) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Please fill title and assignee")
                            }
                            return@Button
                        }

                        isSaving = true
                        scope.launch {
                            try {
                                TaskRepository.createTask(
                                    TaskRow(
                                        title = title,
                                        description = description,
                                        assignedTo = selectedStaffId,
                                        assignedBy = UserSession.get()?.id,
                                        branchId = selectedBranchId,
                                        priority = priority.lowercase(),
                                        status = "pending",
                                        dueDate = dueDateForDb
                                    )
                                )
                                snackbarHostState.showSnackbar("Task assigned successfully")
                                onBack()
                            } catch (_: Exception) {
                                snackbarHostState.showSnackbar("Failed to assign task")
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Cream, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Cream)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Assign Task", color = Cream,
                            fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}