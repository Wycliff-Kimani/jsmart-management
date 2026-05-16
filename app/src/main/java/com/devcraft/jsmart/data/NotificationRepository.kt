package com.devcraft.jsmart.data

import android.util.Log
import com.devcraft.jsmart.supabase
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object NotificationRepository {

    var lastViewedNotification: NotificationRow? = null

    suspend fun getUserIdsByBranch(branchId: String?): List<String> {
        return try {
            if (branchId.isNullOrBlank()) {
                supabase.postgrest["users"]
                    .select { filter { eq("is_active", true) } }
                    .decodeList<UserBrief>()
                    .map { it.id }
            } else {
                supabase.postgrest["users"]
                    .select {
                        filter {
                            eq("branch_id", branchId)
                            eq("is_active", true)
                        }
                    }
                    .decodeList<UserBrief>()
                    .map { it.id }
            }
        } catch (e: Exception) {
            Log.e("NotifRepo", "Fetch users error: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getNotifications(userId: String): List<NotificationRow> {
        return try {
            supabase.postgrest["notifications"]
                .select {
                    filter { eq("user_id", userId) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<NotificationRow>()
        } catch (e: Exception) {
            Log.e("NotifRepo", "Error: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun markAsRead(notificationId: String): Boolean {
        return try {
            supabase.postgrest["notifications"].update(
                { set("is_read", true) }
            ) {
                filter { eq("id", notificationId) }
            }
            true
        } catch (e: Exception) {
            Log.e("NotifRepo", "Mark read error: ${e.message}", e)
            false
        }
    }

    suspend fun markAllAsRead(userId: String): Boolean {
        return try {
            supabase.postgrest["notifications"].update(
                { set("is_read", true) }
            ) {
                filter {
                    eq("user_id", userId)
                    eq("is_read", false)
                }
            }
            true
        } catch (e: Exception) {
            Log.e("NotifRepo", "Mark all read error: ${e.message}", e)
            false
        }
    }

    suspend fun insertNotification(
        userId: String,
        title: String,
        body: String,
        type: String,
        senderName: String? = null,
        referenceId: String? = null
    ) {
        try {
            supabase.postgrest["notifications"].insert(
                NotificationInsert(
                    userId = userId,
                    title = title,
                    body = body,
                    type = type,
                    senderName = senderName,
                    referenceId = referenceId,
                    isRead = false
                )
            )
        } catch (e: Exception) {
            Log.e("NotifRepo", "Insert error: ${e.message}", e)
        }
    }

    suspend fun getNotificationById(id: String): NotificationRow? {
        return try {
            supabase.postgrest["notifications"]
                .select {
                    filter { eq("id", id) }
                }
                .decodeSingleOrNull<NotificationRow>()
        } catch (e: Exception) {
            Log.e("NotifRepo", "Get by id error: ${e.message}", e)
            null
        }
    }

    suspend fun getUnreadCount(userId: String): Int {
        return try {
            val response = supabase.postgrest["notifications"]
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("is_read", false)
                    }
                }
            response.decodeList<NotificationRow>().size
        } catch (_: Exception) {
            0
        }
    }
}
