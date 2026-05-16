package com.devcraft.jsmart.data

import kotlinx.serialization.Serializable

@Serializable
data class ConversationPreview(
    val userId: String,
    val userName: String,
    val lastMessage: String,
    val lastMessageTime: String,
    val isRead: Boolean = true,
    val senderId: String = "",
    val receiverId: String = ""
)
