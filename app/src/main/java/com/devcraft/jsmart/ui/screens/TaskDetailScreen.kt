package com.devcraft.jsmart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.devcraft.jsmart.AppUser
import com.devcraft.jsmart.data.*
import com.devcraft.jsmart.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    task: TaskRow,
    currentUser: AppUser,
    navController: NavController
) {
    var comments by remember { mutableStateOf<List<TaskCommentRow>>(emptyList()) }
    var commentText by remember { mutableStateOf("") }
    var isLoadingComments by remember { mutableStateOf(true) }
    var isSendingComment by remember { mutableStateOf(false) }
    var assigneeName by remember { mutableStateOf("Loading...") }
    var creatorName by remember { mutableStateOf("Loading...") }

    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(task.id) {
        if (task.id != null) {
            val taskComments = TaskRepository.getTaskComments(task.id)
            comments = taskComments
            assigneeName = TaskRepository.resolveUserName(task.assignedTo)
            creatorName = TaskRepository.resolveUserName(task.assignedBy)
            isLoadingComments = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text(task.title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TealPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                modifier = Modifier.imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Add a comment...") },
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = OutlineColor
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val trimmed = commentText.trim()
                            if (trimmed.isNotBlank()) {
                                isSendingComment = true
                                scope.launch {
                                    val success = TaskRepository.addTaskComment(
                                        taskId = task.id!!,
                                        userId = currentUser.id,
                                        userName = currentUser.fullName,
                                        comment = trimmed
                                    )
                                    if (success) {
                                        commentText = ""
                                        @Suppress("AssignedValueIsNeverRead")
                                        comments = TaskRepository.getTaskComments(task.id)

                                        // Notification
                                        val targetUserId = if (task.assignedBy != currentUser.id) task.assignedBy else task.assignedTo
                                        if (targetUserId != null && targetUserId != currentUser.id) {
                                            NotificationRepository.insertNotification(
                                                userId = targetUserId,
                                                title = "New comment on: ${task.title}",
                                                body = "${currentUser.fullName}: $trimmed",
                                                type = "task",
                                                senderName = currentUser.fullName,
                                                referenceId = task.id
                                            )
                                        }
                                    } else {
                                        snackbarHost.showSnackbar("Failed to post comment")
                                    }
                                    isSendingComment = false
                                }
                            }
                        },
                        enabled = !isSendingComment && commentText.isNotBlank(),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = TealPrimary)
                    ) {
                        if (isSendingComment) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = TealPrimary
                            )
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Cream),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                TaskInfoSection(task, assigneeName, creatorName)
            }

            item {
                Text(
                    "Comments (${comments.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = CharcoalDark
                )
            }

            if (isLoadingComments) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TealPrimary)
                    }
                }
            } else if (comments.isEmpty()) {
                item {
                    Text("No comments yet", color = CharcoalMedium)
                }
            } else {
                items(comments) { comment ->
                    CommentBubble(comment, currentUser.id)
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun TaskInfoSection(task: TaskRow, assigneeName: String, creatorName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(task.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CharcoalDark)
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val statusColor = when (task.status.lowercase()) {
                    "pending" -> WarningAmber
                    "in_progress" -> Color(0xFF2196F3)
                    "done" -> SuccessGreen
                    else -> CharcoalMedium
                }
                StatusChip(task.status, statusColor)

                val priorityColor = when (task.priority.lowercase()) {
                    "high" -> ErrorRed
                    "medium" -> WarningAmber
                    else -> SuccessGreen
                }
                StatusChip(task.priority, priorityColor)
            }

            Spacer(modifier = Modifier.height(12.dp))

            InfoRow("Assigned to:", assigneeName)
            InfoRow("Due date:", task.dueDate ?: "No due date")
            InfoRow("Created by:", creatorName)

            if (!task.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Description:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(task.description, fontSize = 14.sp, color = CharcoalMedium)
            }
        }
    }
}

@Composable
fun StatusChip(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text.replace("_", " ").uppercase(),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.width(100.dp))
        Text(value, fontSize = 14.sp, color = CharcoalMedium)
    }
}

@Composable
fun CommentBubble(comment: TaskCommentRow, currentUserId: String) {
    val isMine = comment.userId == currentUserId

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isMine) TealPrimary.copy(alpha = 0.06f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = TealPrimary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    comment.userName?.firstOrNull()?.toString()?.uppercase() ?: "?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(comment.userName ?: "Unknown User", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(formatTaskTime(comment.createdAt), fontSize = 10.sp, color = CharcoalMedium)
            }
            Text(comment.message, fontSize = 14.sp, color = CharcoalDark)
        }
    }
}

fun formatTaskTime(createdAt: String?): String {
    if (createdAt.isNullOrBlank()) return ""
    return try {
        val cleaned = createdAt
            .replace(" ", "T")
            .trim()

        val instant = try {
            Instant.parse(cleaned)
        } catch (_: Exception) {
            try {
                Instant.parse("${cleaned}Z")
            } catch (_: Exception) {
                val truncated = cleaned
                    .replace(
                        Regex("(\\.\\d{3})\\d+Z?$"),
                        "$1Z"
                    )
                Instant.parse(truncated)
            }
        }

        val localDateTime = instant.toLocalDateTime(
            TimeZone.currentSystemDefault()
        )
        val hour = localDateTime.hour
            .toString().padStart(2, '0')
        val minute = localDateTime.minute
            .toString().padStart(2, '0')
        "$hour:$minute"
    } catch (_: Exception) {
        ""
    }
}
