@file:Suppress("SpellCheckingInspection")

package com.devcraft.jsmart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devcraft.jsmart.AppUser
import com.devcraft.jsmart.data.ChatMessage
import com.devcraft.jsmart.data.ChatRepository
import com.devcraft.jsmart.ui.theme.Cream
import com.devcraft.jsmart.ui.theme.TealPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    currentUser: AppUser,
    otherUserId: String,
    otherUserName: String,
    onBack: () -> Unit
) {
    var messages by remember { mutableStateOf(emptyList<ChatMessage>()) }
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        messages = ChatRepository.getDirectMessages(currentUser.id, otherUserId)
        ChatRepository.markDMsAsRead(currentUser.id, otherUserId)
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
        while (true) {
            delay(2000)
            val newMessages = ChatRepository.getDirectMessages(currentUser.id, otherUserId)
            if (newMessages.size != messages.size) {
                messages = newMessages
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(otherUserName, fontWeight = FontWeight.Bold) },
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
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(Cream)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(
                        message = msg.message,
                        senderName = null, // DMs don't need sender names
                        time = msg.createdAt?.let { formatChatTime(it) } ?: "",
                        isCurrentUser = msg.senderId == currentUser.id
                    )
                }
            }

            ChatInput(
                text = text,
                onTextChange = { text = it },
                hint = "Type a message...",
                onSend = {
                    if (text.isNotBlank()) {
                        scope.launch {
                            val success = ChatRepository.sendDirectMessage(
                                senderId = currentUser.id,
                                senderName = currentUser.fullName,
                                receiverId = otherUserId,
                                receiverName = otherUserName,
                                message = text
                            )
                            if (success) {
                                text = ""
                                messages = ChatRepository.getDirectMessages(currentUser.id, otherUserId)
                            }
                        }
                    }
                }
            )
        }
    }
}

fun formatChatTime(createdAt: String?): String {
    if (createdAt.isNullOrBlank()) return ""
    return try {
        val cleaned = createdAt
            .replace(" ", "T")
            .trim()

        val instant = try {
            Instant.parse(cleaned)
        } catch (e1: Exception) {
            try {
                Instant.parse("${cleaned}Z")
            } catch (e2: Exception) {
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
    } catch (e: Exception) {
        Log.e("TimeFormat", "Failed to parse timestamp: $createdAt", e)
        try {
            val t = createdAt.replace("T", " ")
            val timePart = t.substring(11, 16)
            val parts = timePart.split(":")
            val hour = (parts[0].toInt() + 3) % 24
            "${hour.toString().padStart(2, '0')}:${parts[1]}"
        } catch (e2: Exception) {
            ""
        }
    }
}
