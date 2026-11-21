package com.example.data.repository

import com.example.data.model.BeachLocation
import com.example.data.model.LocationSearchResult
import com.example.data.model.MarineConditions
import com.example.data.model.NearbyStation
import com.example.data.model.TideDirection
import com.example.data.model.TideEventType
import com.example.data.model.TideExtremum
import com.example.data.model.TidePoint
import com.example.data.model.TideReport
import com.example.data.model.TideSource
import com.example.data.model.TideUiState
import com.example.data.remote.NetworkModule
import com.example.data.remote.NoaaDataService
import com.example.data.remote.NoaaMetadataService
import com.example.data.remote.NoaaPredictionDto
import com.example.data.remote.NoaaStationDto
import com.example.data.remote.OpenMeteoGeocodingService
import com.example.data.remote.OpenMeteoMarineResponse
import com.example.data.remote.OpenMeteoMarineService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

class TideRepository(
    private val noaaMetadataService: NoaaMetadataService = NetworkModule.noaaMetadataService,
    private val noaaDataService: NoaaDataService = NetworkModule.noaaDataService,
    private val marineService: OpenMeteoMarineService = NetworkModule.openMeteoMarineService,
    private val geocodingService: OpenMeteoGeocodingService = NetworkModule.openMeteoGeocodingService,
) {
    private val stationsMutex = Mutex()
    private var cachedStations: List<NoaaStationDto>? = null

    suspend fun searchLocations(query: String): Result<List<LocationSearchResult>> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext Result.success(emptyList())

        parseCoordinates(trimmed)?.let { (lat, lon) ->
            val pin = BeachLocation(
                id = "coords_${"%.4f".format(Locale.US, lat)}_${"%.4f".format(Locale.US, lon)}",
                name = "Pinned Coordinates",
                subtitle = formatCoords(lat, lon),
                latitude = lat,
                longitude = lon,
            )
            return@withContext Result.success(listOf(LocationSearchResult(location = pin)))
        }

        coroutineScope {
            val geocodingDeferred = async {
                runCatching { geocodingService.searchLocations(name = trimmed, count = 12) }.getOrNull()
            }
            val stationsDeferred = async {
                getNoaaStationsOrEmpty()
            }

            val geoPlaces = geocodingDeferred.await()?.results.orEmpty().map { place ->
                val regionParts = listOfNotNull(place.admin1, place.country).distinct()
                LocationSearchResult(
                    location = BeachLocation(
                        id = "geo_${place.id}",
                        name = place.name,
                        subtitle = regionParts.joinToString(", ").ifBlank { formatCoords(place.latitude, place.longitude) },
                        latitude = place.latitude,
                        longitude = place.longitude,
                    ),
                    isNoaaStation = false,
                )
            }

            val lowerQuery = trimmed.lowercase(Locale.US)
            val stationMatches = stationsDeferred.await()
                .asSequence()
                .filter { station ->
                    val lat = station.lat
                    val lng = station.lng
                    lat != null && lng != null && (
                        station.name.lowercase(Locale.US).contains(lowerQuery) ||
                            (station.state?.lowercase(Locale.US)?.contains(lowerQuery) == true)
                        )
                }
                .take(8)
                .map { station ->
                    LocationSearchResult(
                        location = BeachLocation(
                            id = "noaa_${station.id}",
                            name = station.name,
                            subtitle = listOfNotNull(station.state, "NOAA Station #${station.id}").joinToString(" • "),
                            latitude = station.lat!!,
                            longitude = station.lng!!,
                            noaaStationId = station.id,
                        ),
                        isNoaaStation = true,
                    )
                }
                .toList()

            val combined = (stationMatches + geoPlaces)
                .distinctBy { "${it.location.name.lowercase(Locale.US)}_${"%.2f".format(Locale.US, it.location.latitude)}_${"%.2f".format(Locale.US, it.location.longitude)}" }

            if (combined.isEmpty() && geoPlaces.isEmpty() && stationMatches.isEmpty()) {
                Result.success(emptyList())
            } else {
                Result.success(combined)
            }
        }
    }

    suspend fun fetchTideReport(location: BeachLocation): TideUiState = withContext(Dispatchers.IO) {
        try {
            coroutineScope {
                val stationsDeferred = async { getNoaaStationsOrEmpty() }
                val marineDeferred = async {
                    runCatching {
                        marineService.getMarineForecast(
                            latitude = location.latitude,
                            longitude = location.longitude,
                        )
                    }.getOrNull()
                }

                val stations = stationsDeferred.await()
                val marine = marineDeferred.await()
                val beachUtcOffsetSeconds = marine?.utcOffsetSeconds ?: estimateUtcOffsetSeconds(location.longitude)
                val utcOffsetMillis = beachUtcOffsetSeconds * 1000L
                val timezoneLabel = marine?.timezoneAbbreviation
                    ?.takeIf { it.isNotBlank() }
                    ?: formatUtcOffsetLabel(utcOffsetMillis)
                val beachTimezoneId = marine?.timezone

                val nearbyStations = findNearbyStations(
                    lat = location.latitude,
                    lon = location.longitude,
                    stations = stations,
                    limit = 6,
                )

                val chosenStation = when {
                    location.noaaStationId != null -> {
                        nearbyStations.firstOrNull { it.id == location.noaaStationId }
                            ?: stations.firstOrNull { it.id == location.noaaStationId }?.let { s ->
                                NearbyStation(
                                    id = s.id,
                                    name = s.name,
                                    state = s.state,
                                    latitude = s.lat ?: location.latitude,
                                    longitude = s.lng ?: location.longitude,
                                    distanceKm = haversineKm(location.latitude, location.longitude, s.lat ?: location.latitude, s.lng ?: location.longitude),
                                )
                            }
                    }
                    else -> nearbyStations.firstOrNull()?.takeIf { it.distanceKm <= 65.0 }
                }

                val marineConditions = extractCurrentMarineConditions(marine, utcOffsetMillis)

                if (chosenStation != null) {
                    val noaaReport = fetchNoaaReport(
                        location = location,
                        station = chosenStation,
                        utcOffsetMillis = utcOffsetMillis,
                        beachUtcOffsetSeconds = beachUtcOffsetSeconds,
                        beachTimezoneId = beachTimezoneId,
                        timezoneLabel = timezoneLabel,
                        marineConditions = marineConditions,
                        nearbyStations = nearbyStations,
                    )
                    if (noaaReport != null) {
                        return@coroutineScope TideUiState.Success(noaaReport)
                    }
                }

                if (marine != null) {
                    val globalReport = buildOpenMeteoReport(
                        location = location,
                        marine = marine,
                        utcOffsetMillis = utcOffsetMillis,
                        beachUtcOffsetSeconds = beachUtcOffsetSeconds,
                        beachTimezoneId = beachTimezoneId,
                        timezoneLabel = timezoneLabel,
                        marineConditions = marineConditions,
                        nearbyStations = nearbyStations,
                    )
                    if (globalReport != null) {
                        return@coroutineScope TideUiState.Success(globalReport)
                    }
                }

                val nearestCoastal = nearbyStations.firstOrNull()
                TideUiState.InlandLocation(
                    location = location,
                    nearestStation = nearestCoastal,
                    message = "No marine tide signal is available at ${location.name}. Select a coastal beach or jump to the nearest tide station.",
                )
            }
        } catch (e: Exception) {
            TideUiState.Error(
                message = e.localizedMessage ?: "Unable to load tide data. Check your internet connection and try again.",
                location = location,
            )
        }
    }

    private suspend fun getNoaaStationsOrEmpty(): List<NoaaStationDto> {
        cachedStations?.let { return it }
        return stationsMutex.withLock {
            cachedStations?.let { return it }
            val fetched = runCatching {
                noaaMetadataService.getTidePredictionStations().stations
                    .filter { it.lat != null && it.lng != null }
            }.getOrNull()
            if (!fetched.isNullOrEmpty()) {
                cachedStations = fetched
            }
            fetched.orEmpty()
        }
    }

    private suspend fun fetchNoaaReport(
        location: BeachLocation,
        station: NearbyStation,
        utcOffsetMillis: Long,
        beachUtcOffsetSeconds: Int,
        beachTimezoneId: String?,
        timezoneLabel: String,
        marineConditions: MarineConditions?,
        nearbyStations: List<NearbyStation>,
    ): TideReport? = coroutineScope {
        val now = System.currentTimeMillis()
        val utcFmt = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val beginDate = utcFmt.format(Date(now - 24L * 3600_000L))
        val endDate = utcFmt.format(Date(now + 72L * 3600_000L))

        val hiloDeferred = async {
            runCatching {
                noaaDataService.getPredictions(
                    beginDate = beginDate,
                    endDate = endDate,
                    stationId = station.id,
                    interval = "hilo",
                )
            }.getOrNull()
        }
        val curveDeferred = async {
            runCatching {
                noaaDataService.getPredictions(
                    beginDate = beginDate,
                    endDate = endDate,
                    stationId = station.id,
                    interval = "30",
                )
            }.getOrNull()
        }

        val hiloList = hiloDeferred.await()?.predictions.orEmpty()
        val rawCurveList = curveDeferred.await()?.predictions.orEmpty()

        val extrema = hiloList.mapNotNull { dto ->
            parseNoaaExtremum(dto, utcOffsetMillis)
        }.sortedBy { it.timestampEpochMillis }

        if (extrema.isEmpty() && rawCurveList.isEmpty()) {
            return@coroutineScope null
        }

        val fullCurve = if (rawCurveList.isNotEmpty()) {
            rawCurveList.mapNotNull { dto ->
                parseNoaaPoint(dto, utcOffsetMillis)
            }.sortedBy { it.timestampEpochMillis }
        } else {
            interpolateCosineCurveFromExtrema(extrema, utcOffsetMillis)
        }

        val windowStart = now - 1L * 3600_000L
        val windowEnd = now + 49L * 3600_000L
        val windowCurve = fullCurve.filter { it.timestampEpochMillis in windowStart..windowEnd }
            .ifEmpty { fullCurve }

        val currentHeight = interpolateHeightAt(fullCurve, now)
            ?: extrema.firstOrNull()?.heightMeters
            ?: 0.0
        val nextExtremum = extrema.firstOrNull { it.timestampEpochMillis > now }
        val direction = computeDirection(fullCurve, now, nextExtremum)
        val upcomingExtrema = extrema.filter { it.timestampEpochMillis >= now - 3600_000L }

        TideReport(
            location = location,
            source = TideSource.NOAA_HARMONIC,
            stationName = listOfNotNull(station.name, station.state).joinToString(", "),
            stationDistanceKm = station.distanceKm,
            datumLabel = "MLLW",
            currentHeightMeters = currentHeight,
            currentTimeMillis = now,
            tideDirection = direction,
            nextExtremum = nextExtremum,
            curvePoints = windowCurve,
            extrema = upcomingExtrema,
            marineConditions = marineConditions,
            nearbyStations = nearbyStations,
            timezoneLabel = timezoneLabel,
            beachUtcOffsetSeconds = beachUtcOffsetSeconds,
            beachTimezoneId = beachTimezoneId,
        )
    }

    private fun buildOpenMeteoReport(
        location: BeachLocation,
        marine: OpenMeteoMarineResponse,
        utcOffsetMillis: Long,
        beachUtcOffsetSeconds: Int,
        beachTimezoneId: String?,
        timezoneLabel: String,
        marineConditions: MarineConditions?,
        nearbyStations: List<NearbyStation>,
    ): TideReport? {
        val hourly = marine.hourly ?: return null
        val times = hourly.time
        val seaLevels = hourly.seaLevelHeightMsl ?: return null
        if (times.isEmpty() || seaLevels.isEmpty()) return null

        val rawPoints = mutableListOf<Pair<Long, Double>>()
        val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        for (i in times.indices) {
            val level = seaLevels.getOrNull(i) ?: continue
            val localAsUtcMillis = runCatching { isoFmt.parse(times[i])?.time }.getOrNull() ?: continue
            val epochUtc = localAsUtcMillis - utcOffsetMillis
            rawPoints.add(epochUtc to level)
        }

        if (rawPoints.size < 6) return null

        val extrema = extractParabolicExtrema(rawPoints, utcOffsetMillis)
        val denseCurve = upsampleSmoothCurve(rawPoints, utcOffsetMillis)

        val now = System.currentTimeMillis()
        val windowStart = now - 1L * 3600_000L
        val windowEnd = now + 49L * 3600_000L
        val windowCurve = denseCurve.filter { it.timestampEpochMillis in windowStart..windowEnd }
            .ifEmpty { denseCurve }

        val currentHeight = interpolateHeightAt(denseCurve, now) ?: rawPoints.first().second
        val nextExtremum = extrema.firstOrNull { it.timestampEpochMillis > now }
        val direction = computeDirection(denseCurve, now, nextExtremum)
        val upcomingExtrema = extrema.filter { it.timestampEpochMillis >= now - 3600_000L }

        return TideReport(
            location = location,
            source = TideSource.OPEN_METEO_MARINE,
            stationName = null,
            stationDistanceKm = null,
            datumLabel = "MSL",
            currentHeightMeters = currentHeight,
            currentTimeMillis = now,
            tideDirection = direction,
            nextExtremum = nextExtremum,
            curvePoints = windowCurve,
            extrema = upcomingExtrema,
            marineConditions = marineConditions,
            nearbyStations = nearbyStations,
            timezoneLabel = timezoneLabel,
            beachUtcOffsetSeconds = beachUtcOffsetSeconds,
            beachTimezoneId = beachTimezoneId,
        )
    }

    private fun extractCurrentMarineConditions(
        marine: OpenMeteoMarineResponse?,
        utcOffsetMillis: Long,
    ): MarineConditions? {
        val hourly = marine?.hourly ?: return null
        val times = hourly.time
        if (times.isEmpty()) return null

        val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val now = System.currentTimeMillis()
        var bestIdx = 0
        var bestDiff = Long.MAX_VALUE

        for (i in times.indices) {
            val localAsUtc = runCatching { isoFmt.parse(times[i])?.time }.getOrNull() ?: continue
            val epochUtc = localAsUtc - utcOffsetMillis
            val diff = abs(epochUtc - now)
            if (diff < bestDiff) {
                bestDiff = diff
                bestIdx = i
            }
        }

        val waveHeight = hourly.waveHeight?.getOrNull(bestIdx)
        val wavePeriod = hourly.wavePeriod?.getOrNull(bestIdx)
        val waveDir = hourly.waveDirection?.getOrNull(bestIdx)
        val swellHeight = hourly.swellWaveHeight?.getOrNull(bestIdx)
        val seaTemp = hourly.seaSurfaceTemperature?.getOrNull(bestIdx)

        if (waveHeight == null && wavePeriod == null && swellHeight == null && seaTemp == null) {
            return null
        }

        return MarineConditions(
            waveHeightMeters = waveHeight,
            wavePeriodSeconds = wavePeriod,
            waveDirectionDegrees = waveDir,
            swellHeightMeters = swellHeight,
            seaSurfaceTempCelsius = seaTemp,
        )
    }

    private fun parseNoaaPoint(dto: NoaaPredictionDto, utcOffsetMillis: Long): TidePoint? {
        val gmtFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val epochMillis = runCatching { gmtFmt.parse(dto.time)?.time }.getOrNull() ?: return null
        val height = dto.value.toDoubleOrNull() ?: return null
        return TidePoint(
            timestampEpochMillis = epochMillis,
            formattedTime = formatDisplayTime(epochMillis, utcOffsetMillis),
            heightMeters = height,
        )
    }

    private fun parseNoaaExtremum(dto: NoaaPredictionDto, utcOffsetMillis: Long): TideExtremum? {
        val gmtFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val epochMillis = runCatching { gmtFmt.parse(dto.time)?.time }.getOrNull() ?: return null
        val height = dto.value.toDoubleOrNull() ?: return null
        val eventType = when (dto.type?.uppercase(Locale.US)) {
            "H" -> TideEventType.HIGH
            "L" -> TideEventType.LOW
            else -> return null
        }
        return TideExtremum(
            timestampEpochMillis = epochMillis,
            formattedTime = formatDisplayTime(epochMillis, utcOffsetMillis),
            formattedDate = formatDisplayDate(epochMillis, utcOffsetMillis),
            heightMeters = height,
            type = eventType,
        )
    }

    private fun interpolateCosineCurveFromExtrema(
        extrema: List<TideExtremum>,
        utcOffsetMillis: Long,
    ): List<TidePoint> {
        if (extrema.size < 2) return emptyList()
        val points = mutableListOf<TidePoint>()
        val stepMillis = 30L * 60_000L

        for (i in 0 until extrema.lastIndex) {
            val a = extrema[i]
            val b = extrema[i + 1]
            val duration = (b.timestampEpochMillis - a.timestampEpochMillis).toDouble()
            if (duration <= 0.0) continue

            var t = a.timestampEpochMillis
            while (t < b.timestampEpochMillis) {
                val progress = (t - a.timestampEpochMillis) / duration
                val h = (a.heightMeters + b.heightMeters) / 2.0 +
                    ((a.heightMeters - b.heightMeters) / 2.0) * cos(PI * progress)
                points.add(
                    TidePoint(
                        timestampEpochMillis = t,
                        formattedTime = formatDisplayTime(t, utcOffsetMillis),
                        heightMeters = h,
                    )
                )
                t += stepMillis
            }
        }
        val last = extrema.last()
        points.add(
            TidePoint(
                timestampEpochMillis = last.timestampEpochMillis,
                formattedTime = formatDisplayTime(last.timestampEpochMillis, utcOffsetMillis),
                heightMeters = last.heightMeters,
            )
        )
        return points
    }

    private fun extractParabolicExtrema(
        points: List<Pair<Long, Double>>,
        utcOffsetMillis: Long,
    ): List<TideExtremum> {
        if (points.size < 3) return emptyList()
        val extrema = mutableListOf<TideExtremum>()
        for (i in 1 until points.lastIndex) {
            val (tPrev, yPrev) = points[i - 1]
            val (tCurr, yCurr) = points[i]
            val (tNext, yNext) = points[i + 1]

            val isHigh = yCurr >= yPrev && yCurr > yNext
            val isLow = yCurr <= yPrev && yCurr < yNext
            if (!isHigh && !isLow) continue

            val denom = yPrev - 2.0 * yCurr + yNext
            val dt = ((tNext - tPrev) / 2.0)
            val offsetFraction = if (abs(denom) > 1e-6) {
                (0.5 * (yPrev - yNext) / denom).coerceIn(-0.5, 0.5)
            } else {
                0.0
            }
            val refinedTime = tCurr + (offsetFraction * dt).roundToLong()
            val refinedHeight = if (abs(denom) > 1e-6) {
                yCurr - 0.25 * (yPrev - yNext) * offsetFraction
            } else {
                yCurr
            }

            val eventType = if (isHigh) TideEventType.HIGH else TideEventType.LOW
            extrema.add(
                TideExtremum(
                    timestampEpochMillis = refinedTime,
                    formattedTime = formatDisplayTime(refinedTime, utcOffsetMillis),
                    formattedDate = formatDisplayDate(refinedTime, utcOffsetMillis),
                    heightMeters = refinedHeight,
                    type = eventType,
                )
            )
        }
        return extrema
    }

    private fun upsampleSmoothCurve(
        points: List<Pair<Long, Double>>,
        utcOffsetMillis: Long,
    ): List<TidePoint> {
        if (points.size < 2) return emptyList()
        val result = mutableListOf<TidePoint>()
        for (i in 0 until points.lastIndex) {
            val (t1, y1) = points[i]
            val (t2, y2) = points[i + 1]
            result.add(
                TidePoint(
                    timestampEpochMillis = t1,
                    formattedTime = formatDisplayTime(t1, utcOffsetMillis),
                    heightMeters = y1,
                )
            )
            val midT = (t1 + t2) / 2L
            val midY = (y1 + y2) / 2.0
            result.add(
                TidePoint(
                    timestampEpochMillis = midT,
                    formattedTime = formatDisplayTime(midT, utcOffsetMillis),
                    heightMeters = midY,
                )
            )
        }
        val (lastT, lastY) = points.last()
        result.add(
            TidePoint(
                timestampEpochMillis = lastT,
                formattedTime = formatDisplayTime(lastT, utcOffsetMillis),
                heightMeters = lastY,
            )
        )
        return result
    }

    private fun interpolateHeightAt(curve: List<TidePoint>, timestampMillis: Long): Double? {
        if (curve.isEmpty()) return null
        if (timestampMillis <= curve.first().timestampEpochMillis) return curve.first().heightMeters
        if (timestampMillis >= curve.last().timestampEpochMillis) return curve.last().heightMeters

        for (i in 0 until curve.lastIndex) {
            val a = curve[i]
            val b = curve[i + 1]
            if (timestampMillis in a.timestampEpochMillis..b.timestampEpochMillis) {
                val span = (b.timestampEpochMillis - a.timestampEpochMillis).toDouble()
                if (span <= 0.0) return a.heightMeters
                val ratio = (timestampMillis - a.timestampEpochMillis) / span
                return a.heightMeters + (b.heightMeters - a.heightMeters) * ratio
            }
        }
        return curve.last().heightMeters
    }

    private fun computeDirection(
        curve: List<TidePoint>,
        now: Long,
        nextExtremum: TideExtremum?,
    ): TideDirection {
        val hBefore = interpolateHeightAt(curve, now - 15L * 60_000L)
        val hAfter = interpolateHeightAt(curve, now + 15L * 60_000L)
        if (hBefore != null && hAfter != null) {
            val delta = hAfter - hBefore
            if (delta > 0.015) return TideDirection.RISING
            if (delta < -0.015) return TideDirection.FALLING
        }
        return when (nextExtremum?.type) {
            TideEventType.HIGH -> TideDirection.RISING
            TideEventType.LOW -> TideDirection.FALLING
            null -> TideDirection.SLACK
        }
    }

    private fun findNearbyStations(
        lat: Double,
        lon: Double,
        stations: List<NoaaStationDto>,
        limit: Int,
    ): List<NearbyStation> {
        return stations.asSequence()
            .mapNotNull { s ->
                val sLat = s.lat ?: return@mapNotNull null
                val sLon = s.lng ?: return@mapNotNull null
                val dist = haversineKm(lat, lon, sLat, sLon)
                if (dist <= 300.0) {
                    NearbyStation(
                        id = s.id,
                        name = s.name,
                        state = s.state,
                        latitude = sLat,
                        longitude = sLon,
                        distanceKm = dist,
                    )
                } else {
                    null
                }
            }
            .sortedBy { it.distanceKm }
            .take(limit)
            .toList()
    }

    private fun parseCoordinates(input: String): Pair<Double, Double>? {
        val parts = input.split(",", " ").map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size != 2) return null
        val lat = parts[0].toDoubleOrNull() ?: return null
        val lon = parts[1].toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        return lat to lon
    }

    private fun formatDisplayTime(epochUtcMillis: Long, utcOffsetMillis: Long): String {
        val fmt = SimpleDateFormat("h:mm a", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return fmt.format(Date(epochUtcMillis + utcOffsetMillis))
    }

    private fun formatDisplayDate(epochUtcMillis: Long, utcOffsetMillis: Long): String {
        val fmt = SimpleDateFormat("EEE, MMM d", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return fmt.format(Date(epochUtcMillis + utcOffsetMillis))
    }

    private fun estimateUtcOffsetSeconds(lon: Double): Int {
        val hours = kotlin.math.round(lon / 15.0).toInt().coerceIn(-12, 14)
        return hours * 3600
    }

    private fun formatUtcOffsetLabel(utcOffsetMillis: Long): String {
        val totalMinutes = (utcOffsetMillis / 60_000L).toInt()
        val hours = totalMinutes / 60
        return if (hours >= 0) "UTC+$hours" else "UTC$hours"
    }

    private fun formatCoords(lat: Double, lon: Double): String {
        val latDir = if (lat >= 0) "N" else "S"
        val lonDir = if (lon >= 0) "E" else "W"
        return String.format(Locale.US, "%.3f° %s, %.3f° %s", abs(lat), latDir, abs(lon), lonDir)
    }

    companion object {
        fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val r = 6371.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c
        }
    }
}
