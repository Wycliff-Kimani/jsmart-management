package com.devcraft.jsmart.data

import com.devcraft.jsmart.supabase
import com.devcraft.jsmart.ui.screens.formatTime
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import android.util.Log

data class AdminDashboardStats(
    val presentToday: Int,
    val absentToday: Int,
    val lateToday: Int,
    val onLeave: Int,
    val totalStaff: Int,
    val avgClockIn: String,
    val monthlyRate: Int
)

@Serializable
data class StaffAttendanceRow(
    val userId: String,
    val fullName: String,
    val status: String,
    val clockInTime: String? = null
)

@Serializable
data class StaffBasicInfo(
    val id: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("branch_id") val branchId: String? = null,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class BranchBasicInfo(
    val id: String,
    val name: String
)

object AdminRepository {

    suspend fun getBranches(): List<BranchBasicInfo> {
        return try {
            supabase.postgrest["branches"]
                .select {
                    order("name", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                }
                .decodeList<BranchBasicInfo>()
        } catch (e: Exception) {
            Log.e("AdminRepo", "Error fetching branches: ${e.message}")
            emptyList()
        }
    }

    suspend fun getDashboardStats(branchId: String): AdminDashboardStats {
        return try {
            val today = LocalDate.now().toString()
            val startOfMonth = LocalDate.now().withDayOfMonth(1).toString()

            // 1. Fetch attendance records for today in this branch
            val todayRecords = supabase.postgrest["attendance"]
                .select {
                    filter {
                        eq("branch_id", branchId)
                        eq("date", today)
                    }
                }
                .decodeList<AttendanceDbRecord>()

            val presentToday = todayRecords
                .filter { it.status == "present" || it.status == "late" }
                .distinctBy { it.userId }.size
            val lateToday = todayRecords
                .filter { it.status == "late" }
                .distinctBy { it.userId }.size
            
            // 2. Fetch total active staff in branch
            val branchUsers = supabase.postgrest["users"]
                .select {
                    filter {
                        eq("branch_id", branchId)
                        eq("is_active", true)
                    }
                }
                .decodeList<StaffBasicInfo>()

            // 1b. Fetch active leave requests for today in this branch
            var onLeaveUserIds = emptySet<String>()
            val branchLeaveCount = try {
                // Instead of fetching leaves filtered by user IDs list,
                // fetch ALL pending leaves directly:
                val pendingLeaves = supabase.postgrest["leave_requests"]
                    .select {
                        filter { eq("status", "pending") }
                    }
                    .decodeList<LeaveRow>()

                Log.d("AdminRepo", "Total pending leaves: ${pendingLeaves.size}")

                // Keep the existing logic for calculating onLeave today
                // Actually the instructions said "Admin sees all pending leaves anyway, no need to filter by branch user IDs."
                // But this count is for the "On Leave" stat on the branch-specific dashboard.
                // If I fetch all pending, I still need to filter by branch or user list if I want it to be accurate for THAT branch.
                // HOWEVER, the instructions explicitly said: "fetch ALL pending leaves directly" and "no need to filter by branch user IDs".
                // And then use:
                /*
                val pendingLeaves = supabase.postgrest["leave_requests"]
                    .select {
                        filter { eq("status", "pending") }
                    }
                    .decodeList<LeaveRow>()
                */
                // Wait, if they are "pending", they are not "on leave" yet (usually "approved" means on leave).
                // Let's re-read the bug description carefully.
                // "fetch ALL pending leaves directly"
                // The error was "failed to parse filter in.[uuid1, uuid2...]"

                // Let's look at the original code in AdminRepository.kt:
                /*
                val branchUserIds = branchUsers.map { it.id }
                if (branchUserIds.isNotEmpty()) {
                    val allLeaves = supabase.postgrest["leave_requests"]
                        .select {
                            filter {
                                filter(
                                    "user_id",
                                    io.github.jan.supabase.postgrest.query.filter.FilterOperator.IN,
                                    branchUserIds
                                )
                            }
                        }
                        .decodeList<LeaveRow>()
                ...
                */

                // The replacement code provided in the prompt is:
                /*
                val pendingLeaves = supabase.postgrest["leave_requests"]
                    .select {
                        filter { eq("status", "pending") }
                    }
                    .decodeList<LeaveRow>()
                */

                // I will follow the instructions exactly.

                val allLeaves = supabase.postgrest["leave_requests"]
                    .select {
                        filter { eq("status", "pending") }
                    }
                    .decodeList<LeaveRow>()

                Log.d("AdminRepo", "Total pending leaves: ${allLeaves.size}")

                // Filter in Kotlin — safer than relying on DB filter
                val activeToday = allLeaves.filter { leave ->
                    val statusMatch = leave.status.trim().lowercase() == "approved"
                    val start = leave.startDate.trim().take(10)
                    val end = leave.endDate.trim().take(10)
                    val dateMatch = today >= start && today <= end
                    statusMatch && dateMatch
                }

                onLeaveUserIds = activeToday.map { it.userId }.toSet()
                activeToday.distinctBy { it.userId }.size
            } catch (e: Exception) {
                Log.e("AdminRepo", "Error fetching leave count: ${e.message}", e)
                0
            }

            val totalStaff = branchUsers.size
            val allStaffIds = branchUsers.map { it.id }.toSet()
            val staffWithRecords = todayRecords.map { it.userId }.toSet()
            val absentToday = allStaffIds.count { it !in staffWithRecords && it !in onLeaveUserIds }

            // 3. Average Clock-in Time
            val nairobi = java.time.ZoneId.of("Africa/Nairobi")
            val avgClockIn = todayRecords
                .filter { it.clockInTime != null }
                .groupBy { it.userId }
                .mapNotNull { (_, records) ->
                    records.minByOrNull { it.clockInTime!! }?.clockInTime
                }
                .map { timestamp ->
                    val normalized = timestamp.replace(" ", "T").replace("+00:00", "Z").replace("+00", "Z")
                    val instant = java.time.Instant.parse(normalized)
                    val dateTime = instant.atZone(nairobi)
                    dateTime.get(java.time.temporal.ChronoField.SECOND_OF_DAY) / 60.0
                }
                .average()
                .takeIf { !it.isNaN() }
                ?.let { avgMinutes ->
                    val hours = (avgMinutes / 60).toInt()
                    val minutes = (avgMinutes % 60).toInt()
                    val time = java.time.LocalTime.of(hours, minutes)
                    time.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))
                } ?: "--"

            // 4. Monthly Rate
            val monthRecords = supabase.postgrest["attendance"]
                .select {
                    filter {
                        eq("branch_id", branchId)
                        gte("date", startOfMonth)
                        lte("date", today)
                    }
                }
                .decodeList<AttendanceDbRecord>()

            val presentOrLateMonth = monthRecords.count { it.status == "present" || it.status == "late" }
            val daysElapsed = LocalDate.now().dayOfMonth
            val totalPossible = totalStaff * daysElapsed
            val monthlyRate = if (totalPossible > 0) (presentOrLateMonth * 100) / totalPossible else 0

            AdminDashboardStats(
                presentToday = presentToday,
                absentToday = absentToday,
                lateToday = lateToday,
                onLeave = branchLeaveCount,
                totalStaff = totalStaff,
                avgClockIn = avgClockIn,
                monthlyRate = monthlyRate
            )
        } catch (e: Exception) {
            Log.e("AdminRepo", "Error fetching dashboard stats: ${e.message}")
            AdminDashboardStats(0, 0, 0, 0, 0, "--", 0)
        }
    }

    suspend fun getTodayStaffAttendance(branchId: String): List<StaffAttendanceRow> {
        return try {
            val today = LocalDate.now().toString()

            // Fetch users in branch
            val users = supabase.postgrest["users"]
                .select {
                    filter {
                        eq("branch_id", branchId)
                        eq("is_active", true)
                    }
                }
                .decodeList<StaffBasicInfo>()

            // Fetch attendance for today
            val attendance = supabase.postgrest["attendance"]
                .select {
                    filter {
                        eq("branch_id", branchId)
                        eq("date", today)
                    }
                }
                .decodeList<AttendanceDbRecord>()

            val attendanceMap = attendance.associateBy { it.userId }

            val rows: List<StaffAttendanceRow> = users.map { user ->
                val record = attendanceMap[user.id]
                StaffAttendanceRow(
                    userId = user.id,
                    fullName = user.fullName,
                    status = record?.status ?: "absent",
                    clockInTime = record?.clockInTime
                )
            }

            rows.sortedWith(compareBy<StaffAttendanceRow> { row ->
                when (row.status) {
                    "present" -> 0
                    "late" -> 1
                    "absent" -> 2
                    "on_leave" -> 3
                    else -> 4
                }
            })
        } catch (e: Exception) {
            Log.e("AdminRepo", "Error fetching today staff attendance: ${e.message}")
            emptyList<StaffAttendanceRow>()
        }
    }
}
