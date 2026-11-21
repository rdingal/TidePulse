package com.example.ui.components

import com.example.data.model.TideUnit
import com.example.data.model.TimezoneMode
import com.example.data.model.TimezonePreference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.SimpleTimeZone
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToInt

data class TimezoneOption(
    val zoneId: String,
    val displayName: String,
    val offsetLabel: String,
    val rawOffsetMillis: Int,
)

object TideFormatters {
    private const val METERS_TO_FEET = 3.28084

    fun convertMeters(meters: Double, unit: TideUnit): Double = when (unit) {
        TideUnit.FEET -> meters * METERS_TO_FEET
        TideUnit.METERS -> meters
    }

    fun formatHeight(meters: Double, unit: TideUnit, includeUnit: Boolean = true): String {
        val value = convertMeters(meters, unit)
        val formatted = String.format(Locale.US, "%.2f", value)
        return if (includeUnit) "$formatted ${unit.shortLabel}" else formatted
    }

    fun formatHeightCompact(meters: Double, unit: TideUnit): String {
        val value = convertMeters(meters, unit)
        return String.format(Locale.US, "%.1f %s", value, unit.shortLabel)
    }

    fun formatSignedHeight(meters: Double, unit: TideUnit): String {
        val value = convertMeters(meters, unit)
        val sign = if (value > 0.005) "+" else ""
        return String.format(Locale.US, "%s%.2f %s", sign, value, unit.shortLabel)
    }

    fun formatCountdown(targetEpochMillis: Long, nowEpochMillis: Long = System.currentTimeMillis()): String {
        val diffMillis = targetEpochMillis - nowEpochMillis
        if (diffMillis <= 0L) return "Now"
        val totalMinutes = (diffMillis / 60_000L).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }

    fun formatDistance(distanceKm: Double): String {
        return if (distanceKm < 1.0) {
            "${(distanceKm * 1000).roundToInt()} m away"
        } else {
            val miles = distanceKm * 0.621371
            String.format(Locale.US, "%.1f km (%.1f mi)", distanceKm, miles)
        }
    }

    fun formatTemp(celsius: Double?, unit: TideUnit): String {
        if (celsius == null) return "—"
        return if (unit == TideUnit.FEET) {
            val fahrenheit = celsius * 9.0 / 5.0 + 32.0
            String.format(Locale.US, "%.0f°F", fahrenheit)
        } else {
            String.format(Locale.US, "%.1f°C", celsius)
        }
    }

    fun formatDirectionCompass(degrees: Double?): String {
        if (degrees == null) return ""
        val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val idx = (((degrees % 360.0) + 360.0) % 360.0 / 45.0).roundToInt() % 8
        return "${dirs[idx]} (${degrees.roundToInt()}°)"
    }

    fun formatCoords(lat: Double, lon: Double): String {
        val latDir = if (lat >= 0) "N" else "S"
        val lonDir = if (lon >= 0) "E" else "W"
        return String.format(Locale.US, "%.3f° %s, %.3f° %s", abs(lat), latDir, abs(lon), lonDir)
    }

    fun resolveTimeZone(
        pref: TimezonePreference,
        beachZoneId: String?,
        beachUtcOffsetSeconds: Int,
        beachAbbr: String,
    ): Pair<TimeZone, String> {
        return when (pref.mode) {
            TimezoneMode.BEACH_LOCAL -> {
                val tz = if (!beachZoneId.isNullOrBlank()) {
                    TimeZone.getTimeZone(beachZoneId)
                } else {
                    SimpleTimeZone(beachUtcOffsetSeconds * 1000, beachAbbr)
                }
                val label = beachAbbr.ifBlank { formatOffsetLabel(tz.rawOffset) }
                tz to "Beach ($label)"
            }
            TimezoneMode.DEVICE_LOCAL -> {
                val tz = TimeZone.getDefault()
                val abbr = tz.getDisplayName(tz.inDaylightTime(Date()), TimeZone.SHORT, Locale.US)
                tz to "Device ($abbr)"
            }
            TimezoneMode.UTC -> {
                TimeZone.getTimeZone("UTC") to "UTC"
            }
            TimezoneMode.CUSTOM -> {
                val zoneId = pref.customZoneId ?: TimeZone.getDefault().id
                val tz = TimeZone.getTimeZone(zoneId)
                val city = zoneId.substringAfterLast('/').replace('_', ' ')
                val abbr = tz.getDisplayName(tz.inDaylightTime(Date()), TimeZone.SHORT, Locale.US)
                tz to "$city ($abbr)"
            }
        }
    }

    fun formatTimeInZone(epochMillis: Long, timeZone: TimeZone): String {
        val fmt = SimpleDateFormat("h:mm a", Locale.US).apply {
            this.timeZone = timeZone
        }
        return fmt.format(Date(epochMillis))
    }

    fun formatChartTimeInZone(epochMillis: Long, timeZone: TimeZone): String {
        val fmt = SimpleDateFormat("h:mma", Locale.US).apply {
            this.timeZone = timeZone
        }
        return fmt.format(Date(epochMillis))
            .replace("AM", "a")
            .replace("PM", "p")
    }

    fun formatDateInZone(epochMillis: Long, timeZone: TimeZone): String {
        val fmt = SimpleDateFormat("EEE, MMM d", Locale.US).apply {
            this.timeZone = timeZone
        }
        return fmt.format(Date(epochMillis))
    }

    fun formatOffsetLabel(offsetMillis: Int): String {
        val totalMinutes = offsetMillis / 60_000
        val sign = if (totalMinutes >= 0) "+" else "-"
        val absMinutes = abs(totalMinutes)
        val hours = absMinutes / 60
        val mins = absMinutes % 60
        return if (mins == 0) {
            String.format(Locale.US, "UTC%s%d", sign, hours)
        } else {
            String.format(Locale.US, "UTC%s%d:%02d", sign, hours, mins)
        }
    }

    fun availableTimezoneOptions(): List<TimezoneOption> {
        val now = Date()
        return TimeZone.getAvailableIDs()
            .asSequence()
            .filter { id ->
                id.contains('/') &&
                    !id.startsWith("Etc/") &&
                    !id.startsWith("SystemV/")
            }
            .distinct()
            .map { id ->
                val tz = TimeZone.getTimeZone(id)
                val offset = tz.getOffset(now.time)
                val shortAbbr = tz.getDisplayName(tz.inDaylightTime(now), TimeZone.SHORT, Locale.US)
                val cleanCity = id.replace('_', ' ')
                TimezoneOption(
                    zoneId = id,
                    displayName = "$cleanCity ($shortAbbr)",
                    offsetLabel = formatOffsetLabel(offset),
                    rawOffsetMillis = offset,
                )
            }
            .sortedWith(compareBy<TimezoneOption> { it.rawOffsetMillis }.thenBy { it.displayName })
            .toList()
    }
}
