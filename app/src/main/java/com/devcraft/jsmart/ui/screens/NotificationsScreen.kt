@file:Suppress("SpellCheckingInspection")

package com.devcraft.jsmart.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devcraft.jsmart.AppUser
import com.devcraft.jsmart.data.NotificationRepository
import com.devcraft.jsmart.data.NotificationRow
import com.devcraft.jsmart.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    currentUser: AppUser,
    onBack: () -> Unit,
    onNavigateToDetail: () -> Unit
) {
    var notifications by remember { mutableStateOf<List<NotificationRow>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun loadNotifications() {
        scope.launch {
            isLoading = true
            notifications = NotificationRepository.getNotifications(currentUser.id)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadNotifications()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        scope.launch {
                            NotificationRepository.markAllAsRead(currentUser.id)
                            loadNotifications()
                        }
                    }) {
                        Text("Mark all read", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TealPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Cream)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = TealPrimary)
            } else if (notifications.isEmpty()) {
                Text(
                    "No notifications yet",
                    color = CharcoalMedium,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notifications) { notification ->
                        NotificationCard(
                            notification = notification,
                            onClick = {
                                scope.launch {
                                    NotificationRepository.markAsRead(notification.id)
                                    NotificationRepository.lastViewedNotification = notification
                                    onNavigateToDetail()
                                    notifications = notifications.map {
                                        if (it.id == notification.id) it.copy(isRead = true) else it
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: NotificationRow,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) Color.White else TealPrimary.copy(alpha = 0.06f)
        )
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(if (notification.isRead) Color.Transparent else TealPrimary)
            )

            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                val (icon, color) = when (notification.type.lowercase()) {
                    "task" -> Icons.AutoMirrored.Filled.Assignment to TealPrimary
                    "leave" -> Icons.Filled.EventAvailable to Color(0xFFFF9800)
                    "announcement" -> Icons.Filled.Campaign to Color(0xFF7C3AED)
                    "message" -> Icons.AutoMirrored.Filled.Message to Color(0xFF2563EB)
                    else -> Icons.Filled.Notifications to CharcoalMedium
                }

                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = color.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        notification.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = CharcoalDark
                    )
                    Text(
                        notification.body,
                        fontSize = 13.sp,
                        color = CharcoalMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!notification.senderName.isNullOrBlank()) {
                            Text(
                                notification.senderName,
                                fontSize = 12.sp,
                                color = TealPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            formatNotifTime(notification.createdAt),
                            fontSize = 12.sp,
                            color = CharcoalMedium
                        )
                    }
                }
            }
        }
    }
}

fun formatNotifTime(createdAt: String?): String {
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

        // Use java.time for relative formatting logic
        val dt = LocalDateTime.of(
            localDateTime.year,
            localDateTime.monthNumber,
            localDateTime.dayOfMonth,
            localDateTime.hour,
            localDateTime.minute,
            localDateTime.second
        )
        val now = LocalDateTime.now()
        val duration = Duration.between(dt, now)

        when {
            duration.toMinutes() < 1 -> "Just now"
            duration.toMinutes() < 60 -> "${duration.toMinutes()}m ago"
            duration.toHours() < 24 -> "${duration.toHours()}h ago"
            duration.toDays() == 1L -> "Yesterday"
            else -> dt.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))
        }
    } catch (e: Exception) {
        Log.e("TimeFormat", "Failed to parse timestamp: $createdAt", e)
        ""
    }
}
