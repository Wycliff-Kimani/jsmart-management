package com.devcraft.jsmart.data

import android.util.Log
import com.devcraft.jsmart.supabase
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class GlobalReportsAttendanceSummary(
    val present: Int = 0,
    val absent: Int = 0,
    val late: Int = 0,
    val total: Int = 0,
    val attendanceRate: Int = 0,
    val records: List<AttendanceDbRecord> = emptyList()
)

@Serializable
data class TasksSummary(
    val total: Int = 0,
    val completed: Int = 0,
    val pending: Int = 0,
    val overdue: Int = 0,
    val tasks: List<TaskRow> = emptyList()
)

@Serializable
data class AdminReportStats(
    val presentToday: Int = 0,
    val absentToday: Int = 0,
    val lateToday: Int = 0,
    val totalTasksCompleted: Int = 0,
    val totalTasksPending: Int = 0,
    val pendingLeaveRequests: Int = 0
)

object ReportsRepository {

    // STAFF — Attendance summary for current month
    suspend fun getMyAttendanceSummary(
        userId: String
    ): GlobalReportsAttendanceSummary {
        return try {
            val now = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
            val monthStart = "${now.year}-${now.monthNumber
                .toString().padStart(2,'0')}-01"

            val records = supabase.postgrest["attendance"]
                .select {
                    filter {
                        eq("user_id", userId)
                        gte("date", monthStart)
                    }
                    order("date", Order.DESCENDING)
                }
                .decodeList<AttendanceDbRecord>()

            val present = records.count {
                it.status.lowercase() == "present"
            }
            val absent = records.count {
                it.status.lowercase() == "absent"
            }
            val late = records.count {
                it.status.lowercase() == "late"
            }
            val total = records.size
            val rate = if (total > 0)
                (present * 100 / total) else 0

            GlobalReportsAttendanceSummary(
                present = present,
                absent = absent,
                late = late,
                total = total,
                attendanceRate = rate,
                records = records
            )
        } catch (e: Exception) {
            Log.e("ReportsRepo", "Attendance error: ${e.message}", e)
            GlobalReportsAttendanceSummary()
        }
    }

    // STAFF — My tasks summary
    suspend fun getMyTasksSummary(userId: String): TasksSummary {
        return try {
            val tasks = supabase.postgrest["tasks"]
                .select {
                    filter { eq("assigned_to", userId) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<TaskRow>()

            TasksSummary(
                total = tasks.size,
                completed = tasks.count {
                    it.status.lowercase() == "done"
                },
                pending = tasks.count {
                    it.status.lowercase() == "pending"
                },
                overdue = tasks.count {
                    it.status.lowercase() != "done" &&
                    !it.dueDate.isNullOrBlank() &&
                    it.dueDate < Clock.System.now()
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .date.toString()
                },
                tasks = tasks
            )
        } catch (e: Exception) {
            Log.e("ReportsRepo", "Tasks error: ${e.message}", e)
            TasksSummary()
        }
    }

    // STAFF — My leave history
    suspend fun getMyLeaveHistory(userId: String): List<LeaveRow> {
        return try {
            supabase.postgrest["leave_requests"]
                .select {
                    filter { eq("user_id", userId) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<LeaveRow>()
        } catch (e: Exception) {
            Log.e("ReportsRepo", "Leave error: ${e.message}", e)
            emptyList()
        }
    }

    // ADMIN — Dashboard report stats
    suspend fun getAdminReportStats(
        branchId: String? = null
    ): AdminReportStats {
        return try {
            // Attendance today
            val today = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date.toString()

            val attendanceRecords = if (branchId.isNullOrBlank()) {
                // Super admin — fetch all
                supabase.postgrest["attendance"]
                    .select { filter { eq("date", today) } }
                    .decodeList<AttendanceDbRecord>()
            } else {
                // Fetch attendance for branch users only
                // First get user IDs for this branch
                val branchUserIds = supabase.postgrest["users"]
                    .select {
                        filter {
                            eq("branch_id", branchId)
                            eq("is_active", true)
                        }
                    }
                    .decodeList<UserBrief>()
                    .map { it.id }

                if (branchUserIds.isEmpty()) emptyList()
                else {
                    supabase.postgrest["attendance"]
                        .select { filter { eq("date", today) } }
                        .decodeList<AttendanceDbRecord>()
                        .filter { it.userId in branchUserIds }
                }
            }

            val presentToday = attendanceRecords.count {
                it.status.lowercase() == "present"
            }
            val absentToday = attendanceRecords.count {
                it.status.lowercase() == "absent"
            }
            val lateToday = attendanceRecords.count {
                it.status.lowercase() == "late"
            }

            // Tasks stats
            val allTasks = if (branchId.isNullOrBlank()) {
                supabase.postgrest["tasks"]
                    .select()
                    .decodeList<TaskRow>()
            } else {
                supabase.postgrest["tasks"]
                    .select {
                        filter { eq("branch_id", branchId) }
                    }
                    .decodeList<TaskRow>()
            }
            val completedTasks = allTasks.count {
                it.status.lowercase() == "done"
            }
            val pendingTasks = allTasks.count {
                it.status.lowercase() == "pending"
            }

            // Leave stats
            val allLeaves = supabase.postgrest["leave_requests"]
                .select {
                    filter { eq("status", "pending") }
                }
                .decodeList<LeaveRow>()

            AdminReportStats(
                presentToday = presentToday,
                absentToday = absentToday,
                lateToday = lateToday,
                totalTasksCompleted = completedTasks,
                totalTasksPending = pendingTasks,
                pendingLeaveRequests = allLeaves.size
            )
        } catch (e: Exception) {
            Log.e("ReportsRepo", "Admin stats error: ${e.message}", e)
            AdminReportStats()
        }
    }

    // ADMIN — All staff attendance for a date range
    suspend fun getAllStaffAttendance(
        fromDate: String,
        toDate: String
    ): List<AttendanceDbRecord> {
        return try {
            supabase.postgrest["attendance"]
                .select {
                    filter {
                        gte("date", fromDate)
                        lte("date", toDate)
                    }
                    order("date", Order.DESCENDING)
                }
                .decodeList<AttendanceDbRecord>()
        } catch (e: Exception) {
            Log.e("ReportsRepo", "Staff attendance error: ${e.message}", e)
            emptyList()
        }
    }
}

