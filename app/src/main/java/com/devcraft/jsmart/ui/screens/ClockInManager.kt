package com.devcraft.jsmart.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import com.devcraft.jsmart.UserSession
import com.devcraft.jsmart.supabase
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.coroutines.resume
import kotlin.math.*
import java.time.ZoneId
import java.time.ZonedDateTime

data class BranchLocation(
    val lat: Double,
    val lng: Double,
    val radiusMeters: Double,
    val wifiSSIDs: List<String>
)

val BRANCH_LOCATIONS = mapOf(
    "965dc64f-9636-4062-88dc-a5c727f19245" to BranchLocation(
        lat = -1.169902,
        lng = 36.971323,
        radiusMeters = 100.0,
        wifiSSIDs = listOf("JSMART RECEPTION", "JSMART KAMAKIS", "JSMART LOUNGE")
    ),
    "1ce1489b-8b5b-498e-9d6a-fad2d4000cbe" to BranchLocation(
        lat = -1.2834,
        lng = 36.8172,
        radiusMeters = 100.0,
        wifiSSIDs = listOf("JSMART CBD", "JSMART STORE")
    )
)

fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2).pow(2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}

@Suppress("DEPRECATION")
fun checkWifi(context: Context, allowedSSIDs: List<String>): Boolean {
    return try {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo
        val ssid = info?.ssid?.replace("\"", "")?.trim() ?: return false
        if (ssid == "<unknown ssid>" || ssid.isBlank()) return false
        allowedSSIDs.any { it.equals(ssid, ignoreCase = true) }
    } catch (_: Exception) {
        false
    }
}

suspend fun getCurrentLocation(context: Context): Pair<Double, Double>? {
    if (ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) return null

    val client = LocationServices.getFusedLocationProviderClient(context)
    val cts = CancellationTokenSource()

    return suspendCancellableCoroutine { cont ->
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { location ->
                cont.resume(
                    if (location != null) Pair(location.latitude, location.longitude) else null
                )
            }
            .addOnFailureListener {
                android.util.Log.e("GPS", "Location fetch failed: ${it.message}")
                cont.resume(null)
            }
        cont.invokeOnCancellation { cts.cancel() }
    }
}

fun calculateTotalHours(clockIn: String, clockOut: String): Double {
    return try {
        val normalizedIn = normalizeTimestamp(clockIn)
        val normalizedOut = normalizeTimestamp(clockOut)
        val inTime = java.time.Instant.parse(normalizedIn)
        val outTime = java.time.Instant.parse(normalizedOut)
        val minutes = java.time.Duration.between(inTime, outTime).toMinutes()
        minutes / 60.0
    } catch (_: Exception) {
        0.0
    }
}


@Serializable
data class AttendanceRecord(
    @SerialName("user_id") val userId: String,
    @SerialName("branch_id") val branchId: String,
    @SerialName("department_id") val departmentId: String? = null,
    val date: String,
    @SerialName("clock_in_time") val clockInTime: String? = null,
    @SerialName("clock_out_time") val clockOutTime: String? = null,
    @SerialName("clock_in_lat") val clockInLat: Double? = null,
    @SerialName("clock_in_lng") val clockInLng: Double? = null,
    @SerialName("clock_out_lat") val clockOutLat: Double? = null,
    @SerialName("clock_out_lng") val clockOutLng: Double? = null,
    @SerialName("total_hours") val totalHours: Double? = null,
    @SerialName("wifi_verified") val wifiVerified: Boolean = false,
    @SerialName("geofence_verified") val geofenceVerified: Boolean = false,
    val status: String = "present"
)

data class ClockResult(
    val success: Boolean,
    val message: String,
    val geofenceVerified: Boolean = false,
    val wifiVerified: Boolean = false,
    val distanceMeters: Double = 0.0
)

suspend fun performClockIn(context: Context): ClockResult {
    val user = UserSession.get()
        ?: return ClockResult(false, "Not logged in")

    val branchId = user.branchId
        ?: return ClockResult(false, "No branch assigned to your account")

    val branchLocation = BRANCH_LOCATIONS[branchId]
        ?: return ClockResult(false, "Branch location not configured")

    val location = getCurrentLocation(context)
        ?: return ClockResult(false, "Could not get your location. Make sure GPS is enabled.")

    val distance = calculateDistance(
        location.first, location.second,
        branchLocation.lat, branchLocation.lng
    )

    val geofenceVerified = distance <= branchLocation.radiusMeters
    val wifiVerified = checkWifi(context, branchLocation.wifiSSIDs)

    if (!geofenceVerified) {
        return ClockResult(
            success = false,
            message = "You are ${distance.toInt()}m from the office. " +
                    "Must be within ${branchLocation.radiusMeters.toInt()}m to clock in.",
            geofenceVerified = false,
            wifiVerified = wifiVerified,
            distanceMeters = distance
        )
    }

    return try {
        val today = java.time.LocalDate.now().toString()
        val now = java.time.Instant.now().toString()

        // Check for an open record (clocked in, not yet clocked out)
        val openRecord = supabase.postgrest["attendance"]
            .select {
                filter {
                    eq("user_id", user.id)
                    eq("date", today)
                    filter("clock_out_time", FilterOperator.IS, "null")
                }
                limit(1)
            }
            .decodeList<AttendanceRecord>()

        if (openRecord.isNotEmpty()) {
            // Update the existing open record
            android.util.Log.d("ClockIn", "Updating existing open record")
            supabase.postgrest["attendance"]
                .update({
                    set("clock_in_time", now)
                    set("clock_in_lat", location.first)
                    set("clock_in_lng", location.second)
                    set("wifi_verified", wifiVerified)
                    set("geofence_verified", true)
                    set("status", "present")
                }) {
                    filter {
                        eq("user_id", user.id)
                        eq("date", today)
                        filter("clock_out_time", FilterOperator.IS, "null")
                    }
                }
        } else {
            // New shift — insert fresh record
            android.util.Log.d("ClockIn", "Inserting new clock-in record")
            
            // Determine status based on Nairobi time
            val nairobiZone = ZoneId.of("Africa/Nairobi")
            val nairobiTime = ZonedDateTime.now(nairobiZone)
            val status = if (nairobiTime.hour > 8 || (nairobiTime.hour == 8 && nairobiTime.minute >= 30)) {
                "late"
            } else {
                "present"
            }
            
            val record = AttendanceRecord(
                userId = user.id,
                branchId = branchId,
                departmentId = user.departmentId,
                date = today,
                clockInTime = now,
                clockInLat = location.first,
                clockInLng = location.second,
                wifiVerified = wifiVerified,
                geofenceVerified = true,
                status = status
            )
            supabase.postgrest["attendance"].insert(record)
        }

        ClockResult(
            success = true,
            message = if (wifiVerified)
                "Clocked in ✓ — Inside geofence & on JSmart WiFi"
            else
                "Clocked in ✓ — Inside geofence (not on office WiFi)",
            geofenceVerified = true,
            wifiVerified = wifiVerified,
            distanceMeters = distance
        )
    } catch (e: Exception) {
        android.util.Log.e("ClockIn", "Clock in error: ${e.message}", e)
        ClockResult(false, "Failed to save attendance: ${e.message}")
    }
}

suspend fun performClockOut(context: Context): ClockResult {
    val user = UserSession.get()
        ?: return ClockResult(false, "Not logged in")

    val location = getCurrentLocation(context)

    return try {
        val today = java.time.LocalDate.now().toString()
        val now = java.time.Instant.now().toString()

        // Find the open record for today
        val openRecord = supabase.postgrest["attendance"]
            .select {
                filter {
                    eq("user_id", user.id)
                    eq("date", today)
                    filter("clock_out_time", FilterOperator.IS, "null")
                }
                limit(1)
            }
            .decodeList<AttendanceRecord>()

        android.util.Log.d("ClockOut", "Open records found: ${openRecord.size}")

        if (openRecord.isEmpty()) {
            return ClockResult(false, "No active clock-in found for today")
        }

        val record = openRecord.first()
        val totalHours = if (record.clockInTime != null)
            calculateTotalHours(record.clockInTime, now) else 0.0

        android.util.Log.d("ClockOut",
            "Saving clockOut: $now | totalHours: $totalHours | " +
                    "userId: ${user.id} | date: $today")

        supabase.postgrest["attendance"]
            .update({
                set("clock_out_time", now)
                set("total_hours", totalHours)
                if (location != null) {
                    set("clock_out_lat", location.first)
                    set("clock_out_lng", location.second)
                }
            }) {
                filter {
                    eq("user_id", user.id)
                    eq("date", today)
                    filter("clock_out_time", FilterOperator.IS, "null")
                }
            }

        val h = totalHours.toInt()
        val m = ((totalHours - h) * 60).toInt()
        ClockResult(true, "Clocked out ✓ — ${h}h ${m}m recorded")

    } catch (e: Exception) {
        android.util.Log.e("ClockOut", "Clock out error: ${e.message}", e)
        ClockResult(false, "Failed to record clock out: ${e.message}")
    }
}