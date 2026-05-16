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
import com.devcraft.jsmart.UserSession
import com.devcraft.jsmart.data.TaskRepository
import com.devcraft.jsmart.data.TaskRow
import com.devcraft.jsmart.navigation.Routes
import com.devcraft.jsmart.ui.theme.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.devcraft.jsmart.data.TaskSession
import android.util.Log
import kotlinx.coroutines.launch

@Composable
fun TasksScreen(onNavigate: (String) -> Unit = {}) {
    val currentUser = UserSession.get()
    val userId = currentUser?.id ?: ""
    val actualIsAdmin = currentUser?.role in listOf("admin", "super_admin")

    var selectedTab by remember { mutableIntStateOf(0) }
    var myTasks by remember { mutableStateOf<List<TaskRow>>(emptyList()) }
    var allTasks by remember { mutableStateOf<List<TaskRow>>(emptyList()) }
    var completedTasks by remember { mutableStateOf<List<TaskRow>>(emptyList()) }
    var userMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    val tabs = if (actualIsAdmin)
        listOf("All Tasks", "My Tasks", "Completed")
    else
        listOf("My Tasks", "Completed")

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            isLoading = true
            myTasks = TaskRepository.getMyTasks(userId)
            completedTasks = TaskRepository.getCompletedTasks(userId)
            if (actualIsAdmin) {
                allTasks = TaskRepository.getAllTasks()
                val staffList = com.devcraft.jsmart.data.StaffRepository.getAllStaff()
                userMap = staffList.associate { it.id to it.fullName }
            }
            isLoading = false
        }
    }

    fun reloadTasks() {
        scope.launch {
            isLoading = true
            myTasks = TaskRepository.getMyTasks(userId)
            completedTasks = TaskRepository.getCompletedTasks(userId)
            if (actualIsAdmin) {
                allTasks = TaskRepository.getAllTasks()
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        // Top Bar
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
                Icon(Icons.Filled.CheckCircle, contentDescription = null,
                    tint = Cream, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Tasks", color = Cream,
                    fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            if (actualIsAdmin) {
                IconButton(onClick = { onNavigate(Routes.TASK_CREATION) }) {
                    Icon(Icons.Filled.AddCircle, contentDescription = "Create Task",
                        tint = Cream)
                }
            }
        }

        // Tabs
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
                when {
                    actualIsAdmin && selectedTab == 0 -> AllTasksTab(allTasks, userMap, onNavigate)
                    actualIsAdmin && selectedTab == 1 -> MyTasksTab(myTasks, onNavigate, onTaskUpdate = { reloadTasks() })
                    actualIsAdmin && selectedTab == 2 -> CompletedTasksTab(completedTasks, onNavigate)
                    !actualIsAdmin && selectedTab == 0 -> MyTasksTab(myTasks, onNavigate, onTaskUpdate = { reloadTasks() })
                    !actualIsAdmin && selectedTab == 1 -> CompletedTasksTab(completedTasks, onNavigate)
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun AllTasksTab(tasks: List<TaskRow>, userMap: Map<String, String>, onNavigate: (String) -> Unit) {
    // Stats row
    val totalCount = tasks.size
    val inProgressCount = tasks.count { it.status == "in_progress" || it.status == "pending" }
    val doneCount = tasks.count { it.status == "done" }
    val today = java.time.LocalDate.now().toString()
    val overdueCount = tasks.count { task ->
        task.status != "done" &&
                task.dueDate != null &&
                task.dueDate < today
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard("Total", totalCount)
        StatCard("Active", inProgressCount, Color(0xFF2563EB))
        StatCard("Overdue", overdueCount, Color(0xFFDC2626))
        StatCard("Done", doneCount, Color(0xFF16A34A))
    }

    Spacer(modifier = Modifier.height(20.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("All Staff Tasks", fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold, color = CharcoalDark)
        TextButton(onClick = { onNavigate(Routes.TASK_CREATION) }) {
            Icon(Icons.Filled.Add, contentDescription = null,
                tint = TealPrimary, modifier = Modifier.size(16.dp))
            Text(" Assign Task", color = TealPrimary, fontSize = 13.sp)
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    tasks.forEach { task ->
        TaskCard(task, showAssignee = true, userMap = userMap, onClick = {
            TaskSession.currentTask = task
            onNavigate(Routes.TASK_DETAIL)
        })
    }
}

@Composable
fun MyTasksTab(tasks: List<TaskRow>, onNavigate: (String) -> Unit, onTaskUpdate: () -> Unit) {
    Text("Active Tasks", fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold, color = CharcoalDark)
    Spacer(modifier = Modifier.height(8.dp))

    if (tasks.isEmpty()) {
        Text("No active tasks", fontSize = 14.sp, color = CharcoalMedium)
    }

    tasks.forEach { task ->
        TaskCard(task, showAssignee = false, onClick = {
            TaskSession.currentTask = task
            onNavigate(Routes.TASK_DETAIL)
        }, onMarkComplete = {
            onTaskUpdate()
        })
    }
}

@Composable
fun CompletedTasksTab(tasks: List<TaskRow>, onNavigate: (String) -> Unit) {
    Text("Completed Tasks", fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold, color = CharcoalDark)
    Spacer(modifier = Modifier.height(8.dp))

    if (tasks.isEmpty()) {
        Text("No completed tasks", fontSize = 14.sp, color = CharcoalMedium)
    }

    tasks.forEach { task ->
        TaskCard(task, showAssignee = false, onClick = {
            TaskSession.currentTask = task
            onNavigate(Routes.TASK_DETAIL)
        })
    }
}

@Composable
fun TaskCard(
    task: TaskRow,
    showAssignee: Boolean,
    userMap: Map<String, String> = emptyMap(),
    onClick: () -> Unit = {},
    onMarkComplete: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val priorityColor = when (task.priority.lowercase()) {
        "high" -> ErrorRed
        "medium" -> WarningAmber
        else -> SuccessGreen
    }
    val statusColor = when (task.status.lowercase()) {
        "done" -> SuccessGreen
        "overdue" -> ErrorRed
        "in_progress", "pending" -> TealPrimary
        else -> CharcoalMedium
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(task.title, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold, color = CharcoalDark)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(task.description ?: "", fontSize = 12.sp,
                        color = CharcoalMedium, maxLines = 2)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = priorityColor.copy(alpha = 0.12f)
                ) {
                    Text(task.priority, color = priorityColor,
                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showAssignee) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Person, contentDescription = null,
                                tint = CharcoalMedium, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            val assigneeDisplay = task.assignedTo
                                ?.let { userMap[it] ?: it.take(8) }
                                ?: "Unassigned"
                            Text(assigneeDisplay, fontSize = 11.sp, color = CharcoalMedium)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null,
                            tint = CharcoalMedium, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        val due = task.dueDate ?: "No date"
                        Text("Due $due", fontSize = 11.sp, color = CharcoalMedium)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(task.status, color = statusColor,
                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }

            // Mark complete button for non-completed tasks (staff view)
            if (task.status.lowercase() != "done" && onMarkComplete != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val success = TaskRepository.updateTaskStatus(task.id!!, "done")
                            if (success) {
                                onMarkComplete()
                            } else {
                                // show snackbar if snackbarHostState is accessible (not in this scope),
                                // otherwise just log
                                Log.e("TasksScreen", "Mark complete failed for task ${task.id}")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPrimary)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null,
                        modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mark Complete", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, count: Int, color: Color = CharcoalDark) {
    Card(
        modifier = Modifier.width(78.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = CharcoalMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}