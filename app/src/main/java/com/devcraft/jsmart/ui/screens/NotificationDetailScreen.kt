@file:Suppress("SpellCheckingInspection")

package com.devcraft.jsmart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devcraft.jsmart.AppUser
import com.devcraft.jsmart.data.NotificationRepository
import com.devcraft.jsmart.navigation.Routes
import com.devcraft.jsmart.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDetailScreen(
    currentUser: AppUser,
    onBack: () -> Unit,
    onNavigateToChat: (String, String) -> Unit
) {
    val notification = NotificationRepository.lastViewedNotification
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            if (notification == null) {
                Text("Error: Notification not found", modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val (icon, color) = when (notification.type.lowercase()) {
                        "task" -> Icons.Filled.Assignment to TealPrimary
                        "leave" -> Icons.Filled.EventAvailable to Color(0xFFFF9800)
                        "announcement" -> Icons.Filled.Campaign to Color(0xFF7C3AED)
                        "message" -> Icons.Filled.Message to Color(0xFF2563EB)
                        else -> Icons.Filled.Notifications to CharcoalMedium
                    }

                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        notification.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        notification.body,
                        fontSize = 15.sp,
                        color = CharcoalMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!notification.senderName.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = CharcoalMedium, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("From: ${notification.senderName}", fontSize = 14.sp, color = CharcoalMedium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        formatFullDateTimeManual(notification.createdAt),
                        fontSize = 14.sp,
                        color = CharcoalMedium
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // NOTIFICATION DETAIL — chat deep link:
                    if (notification.type.lowercase() == "message" && !notification.referenceId.isNullOrBlank()) {
                        Button(
                            onClick = {
                                onNavigateToChat(notification.referenceId, notification.senderName ?: "Chat")
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Open Conversation", color = Color.White, modifier = Modifier.padding(8.dp))
                        }
                    }

                    if (!notification.isRead) {
                        Button(
                            onClick = {
                                scope.launch {
                                    NotificationRepository.markAsRead(notification.id)
                                    onBack()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Mark as Read", color = Color.White, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            }
        }
    }
}

fun formatFullDateTimeManual(createdAt: String?): String {
    if (createdAt.isNullOrBlank()) return ""
    return try {
        val instant = Instant.parse(
            createdAt.replace(" ", "T")
                .let { if (!it.endsWith("Z")) "${it}Z" else it }
        )
        val localDateTime = instant.toLocalDateTime(
            TimeZone.currentSystemDefault()
        )

        // Use java.time for formatting
        val dt = LocalDateTime.of(
            localDateTime.year,
            localDateTime.monthNumber,
            localDateTime.dayOfMonth,
            localDateTime.hour,
            localDateTime.minute,
            localDateTime.second
        )
        dt.format(DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a", Locale.ENGLISH))
    } catch (_: Exception) {
        createdAt
    }
}
