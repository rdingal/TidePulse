package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class NoaaStationsResponse(
    val count: Int? = null,
    val stations: List<NoaaStationDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class NoaaStationDto(
    val id: String,
    val name: String,
    val state: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val type: String? = null,
)

@JsonClass(generateAdapter = true)
data class NoaaPredictionsResponse(
    val predictions: List<NoaaPredictionDto>? = null,
    val error: NoaaErrorDto? = null,
)

@JsonClass(generateAdapter = true)
data class NoaaPredictionDto(
    @Json(name = "t") val time: String,
    @Json(name = "v") val value: String,
    val type: String? = null,
)

@JsonClass(generateAdapter = true)
data class NoaaErrorDto(
    val message: String? = null,
)

interface NoaaMetadataService {
    @GET("mdapi/prod/webapi/stations.json")
    suspend fun getTidePredictionStations(
        @Query("type") type: String = "tidepredictions",
    ): NoaaStationsResponse
}

interface NoaaDataService {
    @GET("api/prod/datagetter")
    suspend fun getPredictions(
        @Query("begin_date") beginDate: String,
        @Query("end_date") endDate: String,
        @Query("station") stationId: String,
        @Query("product") product: String = "predictions",
        @Query("datum") datum: String = "MLLW",
        @Query("time_zone") timeZone: String = "gmt",
        @Query("interval") interval: String,
        @Query("units") units: String = "metric",
        @Query("application") application: String = "TidePulseAndroid",
        @Query("format") format: String = "json",
    ): NoaaPredictionsResponse
}
