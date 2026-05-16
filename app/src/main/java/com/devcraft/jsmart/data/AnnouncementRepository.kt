package com.devcraft.jsmart.data

import android.util.Log
import com.devcraft.jsmart.UserRow
import com.devcraft.jsmart.UserSession
import com.devcraft.jsmart.supabase
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class AnnouncementRow(
    val id: String = "",
    val title: String,
    val body: String,
    val priority: String? = null,
    @SerialName("branch_id") val branchId: String? = null,
    @SerialName("posted_by") val postedBy: String,
    @SerialName("is_pinned") val isPinned: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("seen_by") val seenBy: List<String>? = null
)

object AnnouncementSession {
    var current: AnnouncementRow? = null
}

object AnnouncementRepository {

    suspend fun getAnnouncements(): List<AnnouncementRow> {
        return try {
            supabase.postgrest["announcements"]
                .select {
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<AnnouncementRow>()
        } catch (e: Exception) {
            Log.e("AnnouncementRepo", "Error fetching: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getLatestAnnouncement(): AnnouncementRow? {
        return try {
            supabase.postgrest["announcements"]
                .select {
                    order("created_at", Order.DESCENDING)
                    limit(1)
                }
                .decodeSingleOrNull<AnnouncementRow>()
        } catch (e: Exception) {
            Log.e("AnnouncementRepo", "Error fetching latest announcement: ${e.message}")
            null
        }
    }

    suspend fun postAnnouncement(
        title: String,
        body: String,
        branchId: String?,
        postedBy: String
    ): Boolean {
        return try {
            val response = supabase.postgrest["announcements"].insert(mapOf(
                "title" to title,
                "body" to body,
                "branch_id" to branchId,
                "posted_by" to postedBy
            )) {
                select()
            }
            val inserted = response.decodeSingle<AnnouncementRow>()

            // TRIGGER 3 — Announcement posted:
            val posterUserId = UserSession.get()?.id
            val affectedUsers = NotificationRepository
                .getUserIdsByBranch(inserted.branchId)

            affectedUsers
                .filter { it != posterUserId }
                .forEach { userId ->
                    NotificationRepository.insertNotification(
                        userId = userId,
                        title = "New Announcement: ${inserted.title}",
                        body = inserted.body.take(100),
                        type = "announcement",
                        senderName = UserSession.get()?.fullName,
                        referenceId = inserted.id
                    )
                }

            true
        } catch (e: Exception) {
            Log.e("AnnouncementRepo", "Error posting announcement: ${e.message}")
            false
        }
    }

    suspend fun markAsSeen(
        announcementId: String,
        userId: String
    ): Boolean {
        return try {
            // Use Supabase array append
            supabase.postgrest.rpc(
                "append_seen_by",
                buildJsonObject {
                    put("announcement_id", announcementId)
                    put("user_id", userId)
                }
            )
            true
        } catch (e: Exception) {
            Log.e("AnnouncementRepo", "Mark seen error: ${e.message}", e)
            false
        }
    }
}
