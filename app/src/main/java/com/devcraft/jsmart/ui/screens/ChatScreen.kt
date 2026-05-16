@file:Suppress("SpellCheckingInspection")

package com.devcraft.jsmart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devcraft.jsmart.AppUser
import com.devcraft.jsmart.UserRow
import com.devcraft.jsmart.data.*
import com.devcraft.jsmart.supabase
import com.devcraft.jsmart.ui.theme.*
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(currentUser: AppUser, onBack: () -> Unit, onNavigateToDetail: (String, String) -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("General", "Direct Messages")
    var showUserPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showUserPicker = true },
                containerColor = TealPrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "New Message")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(Cream)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = TealPrimary,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color.White
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, color = if (selectedTab == index) Color.White else TealLight) }
                    )
                }
            }

            when (selectedTab) {
                0 -> GeneralChatTab(currentUser)
                1 -> DirectMessagesTab(
                    currentUser = currentUser,
                    onNavigateToDetail = onNavigateToDetail,
                    onNewChatClick = { showUserPicker = true }
                )
            }
        }
    }

    if (showUserPicker) {
        UserPickerSheet(
            currentUser = currentUser,
            onUserSelected = { targetUser ->
                showUserPicker = false
                onNavigateToDetail(targetUser.id, targetUser.fullName)
            },
            onDismiss = { showUserPicker = false }
        )
    }
}

@Composable
fun GeneralChatTab(currentUser: AppUser) {
    var messages by remember { mutableStateOf(emptyList<GroupMessage>()) }
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        messages = ChatRepository.getGroupMessages()
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
        while (true) {
            delay(2000)
            val newMessages = ChatRepository.getGroupMessages()
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

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(
                    message = msg.message,
                    senderName = if (msg.senderId == currentUser.id) null else msg.senderName,
                    time = msg.createdAt?.let { formatChatTime(it) } ?: "",
                    isCurrentUser = msg.senderId == currentUser.id
                )
            }
        }

        ChatInput(
            text = text,
            onTextChange = { text = it },
            hint = "Message everyone...",
            onSend = {
                if (text.isNotBlank()) {
                    scope.launch {
                        val success = ChatRepository.sendGroupMessage(
                            senderId = currentUser.id,
                            senderName = currentUser.fullName,
                            message = text
                        )
                        if (success) {
                            text = ""
                            messages = ChatRepository.getGroupMessages()
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun DirectMessagesTab(
    currentUser: AppUser,
    onNavigateToDetail: (String, String) -> Unit,
    onNewChatClick: () -> Unit
) {
    var conversations by remember { mutableStateOf(emptyList<ConversationPreview>()) }

    LaunchedEffect(Unit) {
        conversations = ChatRepository.getConversations(currentUser.id)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (conversations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No conversations yet", color = CharcoalMedium)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(conversations) { convo ->
                    ConversationItem(convo) {
                        onNavigateToDetail(convo.userId, convo.userName)
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onNewChatClick,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = TealPrimary,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(Icons.Filled.Edit, contentDescription = "New Message")
        }
    }
}

@Composable
fun ConversationItem(convo: ConversationPreview, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = TealLight
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = convo.userName.take(1).uppercase(),
                    color = TealPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = convo.userName,
                fontWeight = if (!convo.isRead) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 16.sp,
                color = CharcoalDark
            )
            Text(
                text = convo.lastMessage,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp,
                color = if (!convo.isRead) CharcoalDark else CharcoalMedium
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatChatTime(convo.lastMessageTime),
                fontSize = 12.sp,
                color = CharcoalMedium
            )
            if (!convo.isRead) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier.size(8.dp).background(TealPrimary, CircleShape)
                )
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = SurfaceContainer)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPickerSheet(
    currentUser: AppUser,
    onUserSelected: (UserRow) -> Unit,
    onDismiss: () -> Unit
) {
    var users by remember { mutableStateOf(emptyList<UserRow>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            users = supabase.postgrest["users"]
                .select()
                .decodeList<UserRow>()
                .filter { it.id != currentUser.id }
            isLoading = false
        } catch (_: Exception) {
            isLoading = false
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp, max = 500.dp).padding(16.dp)) {
            Text("New Conversation", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                LazyColumn {
                    items(users) { user ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onUserSelected(user) }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = TealLight) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(user.fullName.take(1).uppercase(), color = TealPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(user.fullName, fontSize = 16.sp, color = CharcoalDark)
                        }
                        HorizontalDivider(color = SurfaceContainer)
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ChatBubble(
    message: String,
    senderName: String?,
    time: String,
    isCurrentUser: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        if (senderName != null) {
            Text(
                text = senderName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = CharcoalMedium,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }
        Surface(
            color = if (isCurrentUser) TealPrimary else Color(0xFFE0E0E0),
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isCurrentUser) 12.dp else 0.dp,
                bottomEnd = if (isCurrentUser) 0.dp else 12.dp
            )
        ) {
            Text(
                text = message,
                color = if (isCurrentUser) Color.White else CharcoalDark,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                fontSize = 15.sp
            )
        }
        Text(
            text = time,
            fontSize = 10.sp,
            color = CharcoalMedium,
            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
        )
    }
}

@Composable
fun ChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    hint: String,
    onSend: () -> Unit
) {
    Surface(
        color = Color.White,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp).navigationBarsPadding().imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(hint) },
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = OutlineColor
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                colors = IconButtonDefaults.iconButtonColors(contentColor = TealPrimary)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message")
            }
        }
    }
}

fun formatChatTime(timestamp: String): String {
    if (timestamp.isBlank()) return ""
    return try {
        val cleaned = timestamp
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
        try {
            val t = timestamp.replace("T", " ")
            val timePart = t.substring(11, 16)
            val parts = timePart.split(":")
            val hour = (parts[0].toInt() + 3) % 24
            "${hour.toString().padStart(2, '0')}:${parts[1]}"
        } catch (_: Exception) {
            ""
        }
    }
}
