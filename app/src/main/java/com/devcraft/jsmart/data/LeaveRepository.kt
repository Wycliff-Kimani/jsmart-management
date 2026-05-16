package com.devcraft.jsmart.data

import com.devcraft.jsmart.UserSession
import com.devcraft.jsmart.supabase
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import android.util.Log

@Serializable
data class LeaveRow(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("leave_type") val leaveType: String,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    val reason: String? = null,
    val status: String = "pending",
    @SerialName("reviewed_by") val reviewedBy: String? = null,
    @SerialName("reviewed_at") val reviewedAt: String? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class LeaveBalance(
    val annualTotal: Int = 21,
    val annualUsed: Int = 0,
    val annualRemaining: Int = 21,
    val sickTotal: Int = 14,
    val sickUsed: Int = 0,
    val sickRemaining: Int = 14
)

object LeaveRepository {

    suspend fun getLeaveBalance(userId: String): LeaveBalance {
        return try {
            val approved = supabase.postgrest["leave_requests"]
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("status", "approved")
                    }
                }
                .decodeList<LeaveRow>()

            val annualUsed = approved
                .filter { it.leaveType.lowercase() == "annual" }
                .sumOf { calculateDays(it.startDate, it.endDate) }

            val sickUsed = approved
                .filter { it.leaveType.lowercase() == "sick" }
                .sumOf { calculateDays(it.startDate, it.endDate) }

            LeaveBalance(
                annualTotal = 21,
                annualUsed = annualUsed,
                annualRemaining = 21 - annualUsed,
                sickTotal = 14,
                sickUsed = sickUsed,
                sickRemaining = 14 - sickUsed
            )
        } catch (e: Exception) {
            Log.e("LeaveRepo", "Balance error: ${e.message}", e)
            LeaveBalance()
        }
    }

    private fun calculateDays(
        startDate: String?,
        endDate: String?
    ): Int {
        if (startDate == null || endDate == null) return 0
        return try {
            val start = LocalDate.parse(startDate)
            val end = LocalDate.parse(endDate)
            (end.toEpochDay() - start.toEpochDay() + 1)
                .toInt().coerceAtLeast(0)
        } catch (_: Exception) { 0 }
    }

    suspend fun getMyLeaveRequests(userId: String): List<LeaveRow> {
        return try {
            supabase.postgrest["leave_requests"]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<LeaveRow>()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getLeaveForStaff(userId: String): List<LeaveRow> {
        return try {
            supabase.postgrest["leave_requests"]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<LeaveRow>()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getPendingRequests(): List<LeaveRow> {
        return try {
            supabase.postgrest["leave_requests"]
                .select {
                    filter {
                        eq("status", "pending")
                    }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<LeaveRow>()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getAllRequests(): List<LeaveRow> {
        return try {
            supabase.postgrest["leave_requests"]
                .select {
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<LeaveRow>()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun submitLeaveRequest(leave: LeaveRow) {
        try {
            supabase.postgrest["leave_requests"].insert(leave)
        } catch (e: Exception) {
            Log.e("LeaveRepo", "Insert failed", e)
            throw e
        }
    }

    suspend fun approveLeave(leaveId: String, reviewerId: String) {
        try {
            val response = supabase.postgrest["leave_requests"].update(
                {
                    set("status", "approved")
                    set("reviewed_by", reviewerId)
                    set("reviewed_at", Instant.now().toString())
                }
            ) {
                filter {
                    eq("id", leaveId)
                }
                select()
            }
            val updated = response.decodeSingle<LeaveRow>()

            // TRIGGER 2 — Leave approved:
            NotificationRepository.insertNotification(
                userId = updated.userId,
                title = "Leave Request Approved",
                body = "Your leave from ${updated.startDate} to ${updated.endDate} has been approved.",
                type = "leave",
                senderName = UserSession.get()?.fullName,
                referenceId = updated.id
            )
        } catch (e: Exception) {
            Log.e("LeaveRepo", "Approve failed: ${e.message}", e)
        }
    }

    suspend fun rejectLeave(leaveId: String, reviewerId: String, reason: String) {
        try {
            val response = supabase.postgrest["leave_requests"].update(
                {
                    set("status", "rejected")
                    set("reviewed_by", reviewerId)
                    set("reviewed_at", Instant.now().toString())
                    set("rejection_reason", reason)
                }
            ) {
                filter {
                    eq("id", leaveId)
                }
                select()
            }
            val updated = response.decodeSingle<LeaveRow>()

            // TRIGGER 2 — Leave rejected:
            NotificationRepository.insertNotification(
                userId = updated.userId,
                title = "Leave Request Rejected",
                body = "Your leave from ${updated.startDate} to ${updated.endDate} has been rejected.",
                type = "leave",
                senderName = UserSession.get()?.fullName,
                referenceId = updated.id
            )
        } catch (e: Exception) {
            Log.e("LeaveRepo", "Reject failed: ${e.message}", e)
        }
    }
}
