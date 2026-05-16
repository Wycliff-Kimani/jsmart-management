package com.devcraft.jsmart.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    val title: String,
    val body: String,
    val type: String,
    @SerialName("sender_name") val senderName: String? = null,
    @SerialName("reference_id") val referenceId: String? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class NotificationInsert(
    @SerialName("user_id") val userId: String,
    val title: String,
    val body: String,
    val type: String,
    @SerialName("sender_name") val senderName: String? = null,
    @SerialName("reference_id") val referenceId: String? = null,
    @SerialName("is_read") val isRead: Boolean = false
)
