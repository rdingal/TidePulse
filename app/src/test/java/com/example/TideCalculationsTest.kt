package com.example

import com.example.data.model.TideUnit
import com.example.data.model.TimezoneMode
import com.example.data.model.TimezonePreference
import com.example.data.repository.TideRepository
import com.example.ui.components.TideFormatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class TideCalculationsTest {
    @Test
    fun metersToFeetConversion_isAccurate() {
        val feet = TideFormatters.convertMeters(1.0, TideUnit.FEET)
        assertEquals(3.28084, feet, 0.001)
        assertEquals("3.28 ft", TideFormatters.formatHeight(1.0, TideUnit.FEET))
        assertEquals("1.00 m", TideFormatters.formatHeight(1.0, TideUnit.METERS))
    }

    @Test
    fun countdownFormatting_formatsHoursAndMinutes() {
        val now = 1_000_000L
        val target = now + (2L * 60 + 18L) * 60_000L
        assertEquals("2h 18m", TideFormatters.formatCountdown(target, now))
        assertEquals("Now", TideFormatters.formatCountdown(now - 5000L, now))
    }

    @Test
    fun haversineDistance_computesCoastalSeparation() {
        // Santa Monica Pier (34.0086, -118.4980) to Los Angeles / San Pedro (33.7200, -118.2720)
        val distance = TideRepository.haversineKm(34.0086, -118.4980, 33.7200, -118.2720)
        assertTrue(distance in 35.0..42.0)
    }

    @Test
    fun timezoneResolution_formatsConsistentEpochAcrossZones() {
        // 2026-11-20T12:00:00Z
        val epochUtc = 1795176000000L
        val (utcZone, utcLabel) = TideFormatters.resolveTimeZone(
            pref = TimezonePreference(mode = TimezoneMode.UTC),
            beachZoneId = "America/Los_Angeles",
            beachUtcOffsetSeconds = -28800,
            beachAbbr = "PST",
        )
        assertEquals("UTC", utcLabel)
        assertEquals("12:00 PM", TideFormatters.formatTimeInZone(epochUtc, utcZone))

        val (customZone, customLabel) = TideFormatters.resolveTimeZone(
            pref = TimezonePreference(mode = TimezoneMode.CUSTOM, customZoneId = "America/New_York"),
            beachZoneId = "America/Los_Angeles",
            beachUtcOffsetSeconds = -28800,
            beachAbbr = "PST",
        )
        assertTrue(customLabel.contains("New York"))
        assertEquals("7:00 AM", TideFormatters.formatTimeInZone(epochUtc, customZone))
        assertEquals("7:00a", TideFormatters.formatChartTimeInZone(epochUtc, customZone))
    }
}
