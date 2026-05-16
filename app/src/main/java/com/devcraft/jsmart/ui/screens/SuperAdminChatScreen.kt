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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devcraft.jsmart.AppUser
import com.devcraft.jsmart.data.ChatMessage
import com.devcraft.jsmart.data.ChatRepository
import com.devcraft.jsmart.data.ConversationPreview
import com.devcraft.jsmart.data.GroupMessage
import com.devcraft.jsmart.ui.theme.Cream
import com.devcraft.jsmart.ui.theme.TealLight
import com.devcraft.jsmart.ui.theme.TealPrimary
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminChatScreen(
    currentUser: AppUser,
    onBack: () -> Unit,
    onNavigateToDetail: (String, String, String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Group Chat", "Direct Messages")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Conversations", fontWeight = FontWeight.Bold) },
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
                0 -> SuperAdminGroupTab()
                1 -> SuperAdminDirectTab(onNavigateToDetail)
            }
        }
    }
}

@Composable
fun SuperAdminGroupTab() {
    var messages by remember { mutableStateOf(emptyList<GroupMessage>()) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        messages = ChatRepository.getGroupMessages()
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
        while (true) {
            delay(2000)
            messages = ChatRepository.getGroupMessages()
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messages) { msg ->
            ChatBubble(
                message = msg.message,
                senderName = msg.senderName,
                time = msg.createdAt?.let { formatChatTime(it) } ?: "",
                isCurrentUser = false // Everything left-aligned for read-only
            )
        }
    }
}

@Composable
fun SuperAdminDirectTab(onNavigateToDetail: (String, String, String) -> Unit) {
    var conversations by remember { mutableStateOf(emptyList<ConversationPreview>()) }

    LaunchedEffect(Unit) {
        conversations = ChatRepository.getAllConversations()
    }

    if (conversations.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No conversations found")
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(conversations) { convo ->
                ConversationItem(convo) {
                    onNavigateToDetail(convo.senderId, convo.receiverId, convo.userName)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailReadOnly(
    senderId: String,
    receiverId: String,
    displayName: String,
    onBack: () -> Unit
) {
    var messages by remember { mutableStateOf(emptyList<ChatMessage>()) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        // Use getDirectMessages for the two users
        messages = ChatRepository.getDirectMessages(senderId, receiverId)
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(displayName, fontWeight = FontWeight.Bold) },
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
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(padding).fillMaxSize().background(Cream),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(
                    message = msg.message,
                    senderName = msg.senderName,
                    time = msg.createdAt?.let { formatChatTime(it) } ?: "",
                    isCurrentUser = msg.senderId == senderId
                )
            }
        }
    }
}
