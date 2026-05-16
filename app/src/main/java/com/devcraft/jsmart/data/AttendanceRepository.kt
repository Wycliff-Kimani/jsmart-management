package com.devcraft.jsmart.data

import com.devcraft.jsmart.UserSession
import com.devcraft.jsmart.supabase
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class AttendanceDbRecord(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("branch_id") val branchId: String? = null,
    val date: String,
    @SerialName("clock_in_time") val clockInTime: String? = null,
    @SerialName("clock_out_time") val clockOutTime: String? = null,
    val status: String = "present",
    @SerialName("wifi_verified") val wifiVerified: Boolean = false,
    @SerialName("geofence_verified") val geofenceVerified: Boolean = false
)

data class AttendanceSummary(
    val totalDays: Int,
    val presentDays: Int,
    val lateDays: Int,
    val absentDays: Int,
    val attendanceRate: Int
)

object AttendanceRepository {

    suspend fun getMyAttendanceRecords(): List<AttendanceDbRecord> {
        val user = UserSession.get() ?: return emptyList()
        return try {
            supabase.postgrest["attendance"]
                .select {
                    filter { eq("user_id", user.id) }
                    order("date", Order.DESCENDING)
                    limit(30)
                }
                .decodeList<AttendanceDbRecord>()
        } catch (e: Exception) {
            android.util.Log.e("AttendanceRepo", "Error fetching records: ${e.message}")
            emptyList()
        }
    }

    suspend fun getThisMonthSummary(): AttendanceSummary {
        val user = UserSession.get() ?: return AttendanceSummary(0, 0, 0, 0, 0)
        return try {
            val currentMonth = java.time.LocalDate.now().toString().substring(0, 7) // "2026-05"
            val records = supabase.postgrest["attendance"]
                .select {
                    filter {
                        eq("user_id", user.id)
                        gte("date", "$currentMonth-01")
                        lte("date", "$currentMonth-31")
                    }
                }
                .decodeList<AttendanceDbRecord>()

            val present = records.count { it.status == "present" }
            val late = records.count { it.status == "late" }
            val absent = records.count { it.status == "absent" }
            val total = present + late + absent
            val rate = if (total > 0) ((present + late) * 100) / total else 0

            AttendanceSummary(total, present, late, absent, rate)
        } catch (e: Exception) {
            android.util.Log.e("AttendanceRepo", "Error fetching summary: ${e.message}")
            AttendanceSummary(0, 0, 0, 0, 0)
        }
    }
}