package com.devcraft.jsmart.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaskCommentRow(
    val id: String,
    @SerialName("task_id") val taskId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String? = null,
    val message: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class TaskCommentInsert(
    @SerialName("task_id") val taskId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String,
    val message: String
)

