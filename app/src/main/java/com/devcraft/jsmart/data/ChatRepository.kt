package com.devcraft.jsmart.data

import android.util.Log
import com.devcraft.jsmart.supabase
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

object ChatRepository {

    // GROUP MESSAGES
    suspend fun getGroupMessages(): List<GroupMessage> {
        return try {
            supabase.postgrest["group_messages"]
                .select {
                    order("created_at", Order.ASCENDING)
                    limit(100)
                }
                .decodeList<GroupMessage>()
        } catch (e: Exception) {
            Log.e("ChatRepo", "Group fetch error: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun sendGroupMessage(
        senderId: String,
        senderName: String,
        message: String
    ): Boolean {
        return try {
            supabase.postgrest["group_messages"].insert(
                GroupMessageInsert(
                    senderId = senderId,
                    senderName = senderName,
                    message = message
                )
            )
            true
        } catch (e: Exception) {
            Log.e("ChatRepo", "Group send error: ${e.message}", e)
            false
        }
    }

    // DIRECT MESSAGES
    suspend fun getDirectMessages(
        userId: String,
        otherUserId: String
    ): List<ChatMessage> {
        return try {
            supabase.postgrest["chat_messages"]
                .select {
                    order("created_at", Order.ASCENDING)
                    filter {
                        or {
                            and {
                                eq("sender_id", userId)
                                eq("receiver_id", otherUserId)
                            }
                            and {
                                eq("sender_id", otherUserId)
                                eq("receiver_id", userId)
                            }
                        }
                    }
                }
                .decodeList<ChatMessage>()
        } catch (e: Exception) {
            Log.e("ChatRepo", "DM fetch error: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun sendDirectMessage(
        senderId: String,
        senderName: String,
        receiverId: String,
        receiverName: String,
        message: String
    ): Boolean {
        return try {
            supabase.postgrest["chat_messages"].insert(
                ChatMessageInsert(
                    senderId = senderId,
                    senderName = senderName,
                    receiverId = receiverId,
                    receiverName = receiverName,
                    message = message,
                    isRead = false
                )
            )

            // TRIGGER 4 — New direct message:
            try {
                NotificationRepository.insertNotification(
                    userId = receiverId,
                    title = "New Message from $senderName",
                    body = message.take(50),
                    type = "message",
                    senderName = senderName,
                    referenceId = senderId
                )
                Log.d("ChatRepo", "Notification sent to $receiverId")
            } catch (e: Exception) {
                Log.e("ChatRepo", "Notification insert failed: ${e.message}", e)
            }
            true
        } catch (e: Exception) {
            Log.e("ChatRepo", "DM send error: ${e.message}", e)
            false
        }
    }

    suspend fun markDMsAsRead(
        userId: String,
        otherUserId: String
    ) {
        try {
            supabase.postgrest["chat_messages"].update(
                { set("is_read", true) }
            ) {
                filter {
                    eq("receiver_id", userId)
                    eq("sender_id", otherUserId)
                    eq("is_read", false)
                }
            }
        } catch (e: Exception) {
            Log.e("ChatRepo", "Mark read error: ${e.message}", e)
        }
    }

    // CONVERSATIONS LIST (for DMs tab)
    // Returns list of unique users the current user
    // has chatted with, with the last message
    suspend fun getConversations(userId: String): List<ConversationPreview> {
        return try {
            val sent = supabase.postgrest["chat_messages"]
                .select {
                    filter { eq("sender_id", userId) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<ChatMessage>()

            val received = supabase.postgrest["chat_messages"]
                .select {
                    filter { eq("receiver_id", userId) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<ChatMessage>()

            val all = (sent + received)
                .sortedByDescending { it.createdAt }

            // Group by the other person
            val seen = mutableSetOf<String>()
            val previews = mutableListOf<ConversationPreview>()

            for (msg in all) {
                val otherId = if (msg.senderId == userId)
                    msg.receiverId ?: continue
                else msg.senderId
                val otherName = if (msg.senderId == userId)
                    msg.receiverName ?: "Unknown"
                else msg.senderName ?: "Unknown"

                if (otherId !in seen) {
                    seen.add(otherId)
                    previews.add(
                        ConversationPreview(
                            userId = otherId,
                            userName = otherName,
                            lastMessage = msg.message,
                            lastMessageTime = msg.createdAt ?: "",
                            isRead = msg.isRead || msg.senderId == userId,
                            senderId = msg.senderId,
                            receiverId = msg.receiverId ?: ""
                        )
                    )
                }
            }
            previews
        } catch (e: Exception) {
            Log.e("ChatRepo", "Conversations error: ${e.message}", e)
            emptyList()
        }
    }

    // SUPER ADMIN — get ALL conversations
    suspend fun getAllConversations(): List<ConversationPreview> {
        return try {
            val all = supabase.postgrest["chat_messages"]
                .select {
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<ChatMessage>()

            val seen = mutableSetOf<String>()
            val previews = mutableListOf<ConversationPreview>()

            for (msg in all) {
                val key = listOf(msg.senderId, msg.receiverId ?: "")
                    .sorted().joinToString("-")
                if (key !in seen) {
                    seen.add(key)
                    previews.add(
                        ConversationPreview(
                            userId = msg.receiverId ?: "",
                            userName = "${msg.senderName} ↔ ${msg.receiverName}",
                            lastMessage = msg.message,
                            lastMessageTime = msg.createdAt ?: "",
                            isRead = true,
                            senderId = msg.senderId,
                            receiverId = msg.receiverId ?: ""
                        )
                    )
                }
            }
            previews
        } catch (e: Exception) {
            Log.e("ChatRepo", "All convos error: ${e.message}", e)
            emptyList()
        }
    }
}
