package com.devcraft.jsmart.data

import com.devcraft.jsmart.UserSession
import com.devcraft.jsmart.supabase
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import android.util.Log
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaskRow(
    val id: String? = null,
    val title: String,
    val description: String? = null,
    @SerialName("assigned_to") val assignedTo: String? = null,
    @SerialName("assigned_by") val assignedBy: String? = null,
    @SerialName("branch_id") val branchId: String? = null,
    @SerialName("department_id") val departmentId: String? = null,
    val priority: String = "medium",
    val status: String = "pending",
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class UserBrief(
    val id: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("branch_id") val branchId: String? = null
)

object TaskRepository {

    suspend fun getMyTasks(userId: String): List<TaskRow> {
        return try {
            supabase.postgrest["tasks"]
                .select {
                    filter {
                        eq("assigned_to", userId)
                        neq("status", "done")
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<TaskRow>()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getAllTasks(): List<TaskRow> {
        return try {
            supabase.postgrest["tasks"]
                .select {
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<TaskRow>()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getCompletedTasks(userId: String): List<TaskRow> {
        return try {
            supabase.postgrest["tasks"]
                .select {
                    filter {
                        eq("assigned_to", userId)
                        eq("status", "done")
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<TaskRow>()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getTasksForStaff(userId: String): List<TaskRow> {
        return try {
            supabase.postgrest["tasks"]
                .select {
                    filter {
                        eq("assigned_to", userId)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<TaskRow>()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun updateTaskStatus(taskId: String, status: String): Boolean {
        return try {
            supabase.postgrest["tasks"].update(
                {
                    set("status", status)
                }
            ) {
                filter {
                    eq("id", taskId)
                }
            }
            Log.d("TaskRepo", "Status updated to $status for task $taskId")
            true
        } catch (e: Exception) {
            Log.e("TaskRepo", "Failed to update task status", e)
            false
        }
    }

    suspend fun createTask(task: TaskRow) {
        try {
            val response = supabase.postgrest["tasks"].insert(task) {
                select()
            }
            val insertedTask = response.decodeSingle<TaskRow>()

            // TRIGGER 1 — Task assigned:
            NotificationRepository.insertNotification(
                userId = insertedTask.assignedTo ?: "", // the staff member receiving the task
                title = "New Task Assigned",
                body = "You have been assigned: ${insertedTask.title}",
                type = "task",
                senderName = UserSession.get()?.fullName,
                referenceId = insertedTask.id
            )
        } catch (e: Exception) {
            Log.e("TaskRepo", "Failed to create task", e)
        }
    }

    suspend fun getTaskComments(taskId: String): List<TaskCommentRow> {
        return try {
            supabase.postgrest["task_comments"]
                .select {
                    filter { eq("task_id", taskId) }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<TaskCommentRow>()
        } catch (e: Exception) {
            Log.e("TaskRepo", "Comments error: ${e.message}", e)
            emptyList()
        }
    }

    @Suppress("unused")
    suspend fun resolveUserName(userId: String?): String {
        if (userId.isNullOrBlank()) return "Unknown"
        return try {
            val response = supabase.postgrest["users"]
                .select {
                    filter { eq("id", userId) }
                    limit(1)
                }
            val users = response.decodeList<UserBrief>()
            users.firstOrNull()?.fullName ?: "Unknown"
        } catch (_: Exception) {
            "Unknown"
        }
    }

    suspend fun addTaskComment(
        taskId: String,
        userId: String,
        userName: String,
        comment: String
    ): Boolean {
        return try {
            supabase.postgrest["task_comments"].insert(
                TaskCommentInsert(
                    taskId = taskId,
                    userId = userId,
                    userName = userName,
                    message = comment
                )
            )
            Log.d("TaskRepo", "Comment saved for task $taskId")
            true
        } catch (e: Exception) {
            Log.e("TaskRepo", "Add comment FAILED: ${e.message}", e)
            false
        }
    }
}
