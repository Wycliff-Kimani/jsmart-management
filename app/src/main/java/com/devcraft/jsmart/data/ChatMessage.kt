package com.devcraft.jsmart.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String = "",
    @SerialName("sender_id") val senderId: String,
    @SerialName("sender_name") val senderName: String? = null,
    @SerialName("receiver_id") val receiverId: String? = null,
    @SerialName("receiver_name") val receiverName: String? = null,
    val message: String,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ChatMessageInsert(
    @SerialName("sender_id") val senderId: String,
    @SerialName("sender_name") val senderName: String,
    @SerialName("receiver_id") val receiverId: String,
    @SerialName("receiver_name") val receiverName: String,
    val message: String,
    @SerialName("is_read") val isRead: Boolean = false
)

@Serializable
data class GroupMessage(
    val id: String = "",
    @SerialName("sender_id") val senderId: String,
    @SerialName("sender_name") val senderName: String? = null,
    val message: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class GroupMessageInsert(
    @SerialName("sender_id") val senderId: String,
    @SerialName("sender_name") val senderName: String,
    val message: String
)
