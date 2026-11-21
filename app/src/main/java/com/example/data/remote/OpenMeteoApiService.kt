package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class OpenMeteoMarineResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    @Json(name = "utc_offset_seconds") val utcOffsetSeconds: Int? = 0,
    val timezone: String? = null,
    @Json(name = "timezone_abbreviation") val timezoneAbbreviation: String? = null,
    val hourly: MarineHourlyDto? = null,
)

@JsonClass(generateAdapter = true)
data class MarineHourlyDto(
    val time: List<String> = emptyList(),
    @Json(name = "sea_level_height_msl") val seaLevelHeightMsl: List<Double?>? = null,
    @Json(name = "wave_height") val waveHeight: List<Double?>? = null,
    @Json(name = "wave_period") val wavePeriod: List<Double?>? = null,
    @Json(name = "wave_direction") val waveDirection: List<Double?>? = null,
    @Json(name = "swell_wave_height") val swellWaveHeight: List<Double?>? = null,
    @Json(name = "sea_surface_temperature") val seaSurfaceTemperature: List<Double?>? = null,
)

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    val results: List<GeocodingPlaceDto>? = null,
)

@JsonClass(generateAdapter = true)
data class GeocodingPlaceDto(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    @Json(name = "country_code") val countryCode: String? = null,
    val admin1: String? = null,
    val admin2: String? = null,
    val timezone: String? = null,
)

interface OpenMeteoMarineService {
    @GET("v1/marine")
    suspend fun getMarineForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String = "sea_level_height_msl,wave_height,wave_period,wave_direction,swell_wave_height,sea_surface_temperature",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 3,
    ): OpenMeteoMarineResponse
}

interface OpenMeteoGeocodingService {
    @GET("v1/search")
    suspend fun searchLocations(
        @Query("name") name: String,
        @Query("count") count: Int = 12,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json",
    ): GeocodingResponse
}
