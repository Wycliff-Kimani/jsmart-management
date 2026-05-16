package com.devcraft.jsmart.ui.screens

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.Duration
import java.time.LocalDateTime
import java.time.OffsetDateTime

fun normalizeTimestamp(timestamp: String): String {
    return timestamp
        .replace(" ", "T")
        .replace("+00:00", "Z")
        .replace("+00", "Z")
}

fun formatTime(isoString: String?): String {
    if (isoString == null) return "-"
    return try {
        val normalized = normalizeTimestamp(isoString)
        val instant = Instant.parse(normalized)
        val nairobi = ZoneId.of("Africa/Nairobi")
        instant.atZone(nairobi).format(DateTimeFormatter.ofPattern("h:mm a"))
    } catch (_: Exception) { "-" }
}

fun formatDate(dateString: String): String {
    return try {
        val date = java.time.LocalDate.parse(dateString)
        date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
    } catch (_: Exception) { dateString }
}

fun calculateHours(clockIn: String?, clockOut: String?): String {
    if (clockIn == null || clockOut == null) return "0h 00m"
    return try {
        val inTime = Instant.parse(normalizeTimestamp(clockIn))
        val outTime = Instant.parse(normalizeTimestamp(clockOut))
        val minutes = java.time.Duration.between(inTime, outTime).toMinutes()
        val h = minutes / 60
        val m = minutes % 60
        "${h}h ${m.toString().padStart(2, '0')}m"
    } catch (_: Exception) {
        "0h 00m"
    }
}

fun formatRelativeTime(isoString: String?): String {
    if (isoString == null) return ""
    return try {
        val normalized = normalizeTimestamp(isoString)
        val time = OffsetDateTime.parse(normalized).toInstant().atZone(ZoneId.of("Africa/Nairobi")).toLocalDateTime()
        val now = LocalDateTime.now(ZoneId.of("Africa/Nairobi"))
        val duration = Duration.between(time, now)

        when {
            duration.toMinutes() < 1 -> "Just now"
            duration.toMinutes() < 60 -> "${duration.toMinutes()}m ago"
            duration.toHours() < 24 -> "${duration.toHours()}h ago"
            duration.toDays() == 1L -> "Yesterday"
            duration.toDays() < 7 -> "${time.format(DateTimeFormatter.ofPattern("EEEE"))}"
            else -> time.format(DateTimeFormatter.ofPattern("MMM d"))
        }
    } catch (e: Exception) {
        ""
    }
}

fun formatFullDateTime(isoString: String?): String {
    if (isoString == null) return ""
    return try {
        val normalized = normalizeTimestamp(isoString)
        val time = OffsetDateTime.parse(normalized).toInstant().atZone(ZoneId.of("Africa/Nairobi"))
        time.format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a"))
    } catch (e: Exception) {
        ""
    }
}
