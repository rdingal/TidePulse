package com.example.data.model

import com.squareup.moshi.JsonClass

enum class TideUnit(val shortLabel: String) {
    FEET("ft"),
    METERS("m"),
}

enum class TideSource(val label: String) {
    NOAA_HARMONIC("NOAA Harmonic"),
    OPEN_METEO_MARINE("Global Marine"),
}

enum class TideDirection {
    RISING,
    FALLING,
    SLACK,
}

enum class TideEventType {
    HIGH,
    LOW,
}

enum class TimezoneMode {
    BEACH_LOCAL,
    DEVICE_LOCAL,
    UTC,
    CUSTOM,
}

@JsonClass(generateAdapter = true)
data class TimezonePreference(
    val mode: TimezoneMode = TimezoneMode.BEACH_LOCAL,
    val customZoneId: String? = null,
)

@JsonClass(generateAdapter = true)
data class BeachLocation(
    val id: String,
    val name: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
    val noaaStationId: String? = null,
    val isGps: Boolean = false,
)

data class TidePoint(
    val timestampEpochMillis: Long,
    val formattedTime: String,
    val heightMeters: Double,
)

data class TideExtremum(
    val timestampEpochMillis: Long,
    val formattedTime: String,
    val formattedDate: String,
    val heightMeters: Double,
    val type: TideEventType,
)

data class MarineConditions(
    val waveHeightMeters: Double?,
    val wavePeriodSeconds: Double?,
    val waveDirectionDegrees: Double?,
    val swellHeightMeters: Double?,
    val seaSurfaceTempCelsius: Double?,
)

data class NearbyStation(
    val id: String,
    val name: String,
    val state: String?,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double,
)

data class LocationSearchResult(
    val location: BeachLocation,
    val isNoaaStation: Boolean = false,
    val distanceKm: Double? = null,
)

data class TideReport(
    val location: BeachLocation,
    val source: TideSource,
    val stationName: String?,
    val stationDistanceKm: Double?,
    val datumLabel: String,
    val currentHeightMeters: Double,
    val currentTimeMillis: Long,
    val tideDirection: TideDirection,
    val nextExtremum: TideExtremum?,
    val curvePoints: List<TidePoint>,
    val extrema: List<TideExtremum>,
    val marineConditions: MarineConditions?,
    val nearbyStations: List<NearbyStation>,
    val timezoneLabel: String,
    val beachUtcOffsetSeconds: Int = 0,
    val beachTimezoneId: String? = null,
)

sealed interface TideUiState {
    data object AwaitingLocation : TideUiState
    data object Loading : TideUiState
    data class Success(val report: TideReport) : TideUiState
    data class InlandLocation(
        val location: BeachLocation,
        val nearestStation: NearbyStation?,
        val message: String,
    ) : TideUiState
    data class Error(
        val message: String,
        val location: BeachLocation? = null,
    ) : TideUiState
}
