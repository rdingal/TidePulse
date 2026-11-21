package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Anchor
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Water
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BeachLocation
import com.example.data.model.MarineConditions
import com.example.data.model.NearbyStation
import com.example.data.model.TideDirection
import com.example.data.model.TideEventType
import com.example.data.model.TideExtremum
import com.example.data.model.TideReport
import com.example.data.model.TideUiState
import com.example.data.model.TideUnit
import com.example.data.model.TimezonePreference
import com.example.ui.components.TideFormatters
import com.example.ui.components.TideWaveChart
import com.example.ui.theme.AbyssNavy
import com.example.ui.theme.DeepOcean
import com.example.ui.theme.LocalTideExtendedColors
import com.example.ui.theme.ReefTeal
import java.util.TimeZone

@Composable
fun TideDashboardScreen(
    uiState: TideUiState,
    tideUnit: TideUnit,
    timezonePreference: TimezonePreference,
    curveWindowHours: Int,
    savedBeaches: List<BeachLocation>,
    featuredBeaches: List<BeachLocation>,
    isLocatingGps: Boolean,
    gpsMessage: String?,
    onUseGpsClick: () -> Unit,
    onOpenSearchClick: () -> Unit,
    onOpenFavoritesClick: () -> Unit,
    onOpenTimezonePicker: () -> Unit,
    onSelectBeach: (BeachLocation) -> Unit,
    onSelectNearbyStation: (NearbyStation) -> Unit,
    onToggleSaveBeach: (BeachLocation) -> Unit,
    onChangeUnit: (TideUnit) -> Unit,
    onChangeCurveWindow: (Int) -> Unit,
    onRefreshClick: () -> Unit,
    onDismissGpsMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeReport = (uiState as? TideUiState.Success)?.report
    val (activeTimeZone, activeTimezoneLabel) = remember(
        timezonePreference,
        activeReport?.beachTimezoneId,
        activeReport?.beachUtcOffsetSeconds,
        activeReport?.timezoneLabel,
    ) {
        TideFormatters.resolveTimeZone(
            pref = timezonePreference,
            beachZoneId = activeReport?.beachTimezoneId,
            beachUtcOffsetSeconds = activeReport?.beachUtcOffsetSeconds ?: 0,
            beachAbbr = activeReport?.timezoneLabel ?: "",
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "top_controls") {
                DashboardTopBar(
                    tideUnit = tideUnit,
                    activeTimezoneLabel = activeTimezoneLabel,
                    isLocatingGps = isLocatingGps,
                    onUseGpsClick = onUseGpsClick,
                    onOpenSearchClick = onOpenSearchClick,
                    onOpenTimezonePicker = onOpenTimezonePicker,
                    onChangeUnit = onChangeUnit,
                )
            }

            if (savedBeaches.isNotEmpty()) {
                item(key = "favorites_quick_strip") {
                    FavoritesQuickStrip(
                        savedBeaches = savedBeaches,
                        currentLocation = activeReport?.location,
                        onSelectBeach = onSelectBeach,
                        onOpenFavoritesClick = onOpenFavoritesClick,
                    )
                }
            }

            if (gpsMessage != null) {
                item(key = "gps_banner") {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = gpsMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "Dismiss",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { onDismissGpsMessage() }
                                    .padding(start = 12.dp, top = 6.dp, bottom = 6.dp),
                            )
                        }
                    }
                }
            }

            when (uiState) {
                is TideUiState.AwaitingLocation -> {
                    item(key = "onboarding") {
                        OnboardingLocationCard(
                            featuredBeaches = featuredBeaches,
                            isLocatingGps = isLocatingGps,
                            onUseGpsClick = onUseGpsClick,
                            onOpenSearchClick = onOpenSearchClick,
                            onSelectBeach = onSelectBeach,
                        )
                    }
                }

                is TideUiState.Loading -> {
                    item(key = "loading") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 56.dp, horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "Syncing coastal tide stations & marine buoys…",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }

                is TideUiState.InlandLocation -> {
                    item(key = "inland") {
                        InlandLocationCard(
                            state = uiState,
                            onJumpToStation = onSelectNearbyStation,
                            onOpenSearchClick = onOpenSearchClick,
                            onSelectBeach = onSelectBeach,
                            featuredBeaches = featuredBeaches,
                        )
                    }
                }

                is TideUiState.Error -> {
                    item(key = "error") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOff,
                                    contentDescription = "Tide data unavailable",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(40.dp),
                                )
                                Text(
                                    text = "Unable to Fetch Tide Data",
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Text(
                                    text = uiState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = onRefreshClick,
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("retry_tide_button"),
                                    ) {
                                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Retry")
                                    }
                                    OutlinedButton(
                                        onClick = onOpenSearchClick,
                                        modifier = Modifier.heightIn(min = 48.dp),
                                    ) {
                                        Text("Search Beach")
                                    }
                                }
                            }
                        }
                    }
                }

                is TideUiState.Success -> {
                    val report = uiState.report
                    val isSaved = savedBeaches.any {
                        it.id == report.location.id ||
                            (it.name == report.location.name && kotlin.math.abs(it.latitude - report.location.latitude) < 0.01)
                    }

                    item(key = "hero_tide_card") {
                        HeroTideCard(
                            report = report,
                            tideUnit = tideUnit,
                            timeZone = activeTimeZone,
                            timezoneBadgeLabel = activeTimezoneLabel,
                            isSaved = isSaved,
                            onToggleSave = { onToggleSaveBeach(report.location) },
                            onRefresh = onRefreshClick,
                        )
                    }

                    item(key = "tide_curve_card") {
                        TideCurveCard(
                            report = report,
                            tideUnit = tideUnit,
                            windowHours = curveWindowHours,
                            timeZone = activeTimeZone,
                            timezoneBadgeLabel = activeTimezoneLabel,
                            onChangeWindow = onChangeCurveWindow,
                            onOpenTimezonePicker = onOpenTimezonePicker,
                        )
                    }

                    if (report.extrema.isNotEmpty()) {
                        item(key = "extrema_header") {
                            SectionHeader(
                                title = "High & Low Tide Schedule",
                                subtitle = "${report.datumLabel} datum • Shown in $activeTimezoneLabel",
                            )
                        }

                        items(
                            items = report.extrema.take(10),
                            key = { "extremum_${it.timestampEpochMillis}_${it.type.name}" },
                        ) { extremum ->
                            TideExtremumRow(
                                extremum = extremum,
                                unit = tideUnit,
                                timeZone = activeTimeZone,
                                nowMillis = report.currentTimeMillis,
                            )
                        }
                    }

                    report.marineConditions?.let { marine ->
                        item(key = "marine_header") {
                            SectionHeader(
                                title = "Surf & Ocean Telemetry",
                                subtitle = "Real-time coastal wave & surface conditions",
                            )
                        }
                        item(key = "marine_grid") {
                            MarineConditionsGrid(
                                conditions = marine,
                                unit = tideUnit,
                            )
                        }
                    }

                    if (report.nearbyStations.isNotEmpty()) {
                        item(key = "nearby_header") {
                            SectionHeader(
                                title = "Nearby NOAA Tide Stations",
                                subtitle = "Tap any station for exact harmonic predictions",
                            )
                        }
                        item(key = "nearby_stations") {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(vertical = 2.dp),
                            ) {
                                items(
                                    items = report.nearbyStations,
                                    key = { "nearby_${it.id}" },
                                ) { station ->
                                    NearbyStationCard(
                                        station = station,
                                        isCurrentStation = report.location.noaaStationId == station.id ||
                                            report.stationName?.startsWith(station.name) == true,
                                        onClick = { onSelectNearbyStation(station) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DashboardTopBar(
    tideUnit: TideUnit,
    activeTimezoneLabel: String,
    isLocatingGps: Boolean,
    onUseGpsClick: () -> Unit,
    onOpenSearchClick: () -> Unit,
    onOpenTimezonePicker: () -> Unit,
    onChangeUnit: (TideUnit) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(38.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Waves,
                            contentDescription = "TidePulse logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Column {
                    Text(
                        text = "TidePulse",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Coastal Tide Telemetry",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                UnitSegmentedPill(
                    selectedUnit = tideUnit,
                    onSelectUnit = onChangeUnit,
                )
                FilledTonalButton(
                    onClick = onUseGpsClick,
                    enabled = !isLocatingGps,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("use_gps_button"),
                ) {
                    if (isLocatingGps) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.MyLocation,
                            contentDescription = "Use current GPS location",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isLocatingGps) "Locating" else "GPS",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }

        // Quick Search & Timezone row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                onClick = onOpenSearchClick,
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("dashboard_search_bar"),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search beaches",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Search beach or city…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Surface(
                onClick = onOpenTimezonePicker,
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("dashboard_timezone_button"),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = "Set timezone",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = activeTimezoneLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 110.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoritesQuickStrip(
    savedBeaches: List<BeachLocation>,
    currentLocation: BeachLocation?,
    onSelectBeach: (BeachLocation) -> Unit,
    onOpenFavoritesClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = "Favorite beaches",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "Favorite Beaches",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = "Manage",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onOpenFavoritesClick() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .testTag("manage_favorites_link"),
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp),
        ) {
            items(
                items = savedBeaches,
                key = { "quick_fav_${it.id}" },
            ) { beach ->
                val isSelected = currentLocation != null && (
                    currentLocation.id == beach.id ||
                        (currentLocation.name == beach.name && kotlin.math.abs(currentLocation.latitude - beach.latitude) < 0.01)
                    )
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectBeach(beach) },
                    label = {
                        Text(
                            text = beach.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    modifier = Modifier.testTag("quick_favorite_${beach.id}"),
                )
            }
        }
    }
}

@Composable
private fun UnitSegmentedPill(
    selectedUnit: TideUnit,
    onSelectUnit: (TideUnit) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.heightIn(min = 48.dp),
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TideUnit.entries.forEach { unit ->
                val isSelected = unit == selectedUnit
                Surface(
                    onClick = { onSelectUnit(unit) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .testTag("unit_toggle_${unit.shortLabel}"),
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = unit.shortLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroTideCard(
    report: TideReport,
    tideUnit: TideUnit,
    timeZone: TimeZone,
    timezoneBadgeLabel: String,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onRefresh: () -> Unit,
) {
    val (directionLabel, directionIcon, directionColor) = when (report.tideDirection) {
        TideDirection.RISING -> Triple("Rising Tide", Icons.AutoMirrored.Rounded.TrendingUp, ReefTeal)
        TideDirection.FALLING -> Triple("Falling Tide", Icons.AutoMirrored.Rounded.TrendingDown, Color(0xFFFF8C7A))
        TideDirection.SLACK -> Triple("Slack Tide", Icons.AutoMirrored.Rounded.TrendingFlat, Color(0xFFFFD166))
    }

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_tide_card"),
        colors = CardDefaults.cardColors(containerColor = DeepOcean),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_coastal_hero),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.42f,
                modifier = Modifier.matchParentSize(),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                AbyssNavy.copy(alpha = 0.72f),
                                DeepOcean.copy(alpha = 0.88f),
                                AbyssNavy.copy(alpha = 0.96f),
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (report.location.isGps) {
                                Icon(
                                    imageVector = Icons.Rounded.GpsFixed,
                                    contentDescription = "GPS location",
                                    tint = ReefTeal,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Text(
                                text = report.location.name,
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = report.location.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFB5D6E6),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Row {
                        IconButton(
                            onClick = onToggleSave,
                            modifier = Modifier.testTag("favorite_beach_button"),
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isSaved) "Remove from Favorites" else "Add to Favorites",
                                tint = if (isSaved) Color(0xFFFF7B6B) else Color.White,
                            )
                        }
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.testTag("refresh_tide_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh tide report",
                                tint = Color.White,
                            )
                        }
                    }
                }

                // Current height + direction badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "CURRENT TIDE LEVEL",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF93BDD1),
                        )
                        Text(
                            text = TideFormatters.formatSignedHeight(report.currentHeightMeters, tideUnit),
                            style = MaterialTheme.typography.displayLarge,
                            color = Color.White,
                            modifier = Modifier.testTag("current_tide_height_text"),
                        )
                    }

                    Surface(
                        color = directionColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            directionColor.copy(alpha = 0.55f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = directionIcon,
                                contentDescription = directionLabel,
                                tint = directionColor,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = directionLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = directionColor,
                            )
                        }
                    }
                }

                // Next Extremum & Station Telemetry strip
                Surface(
                    color = Color.White.copy(alpha = 0.08f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        report.nextExtremum?.let { next ->
                            val nextTypeLabel = if (next.type == TideEventType.HIGH) "Next High Tide" else "Next Low Tide"
                            val countdown = TideFormatters.formatCountdown(next.timestampEpochMillis, report.currentTimeMillis)
                            val formattedTime = TideFormatters.formatTimeInZone(next.timestampEpochMillis, timeZone)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "$nextTypeLabel in $countdown",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                )
                                Text(
                                    text = "$formattedTime • ${TideFormatters.formatHeight(next.heightMeters, tideUnit)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ReefTeal,
                                )
                            }
                        }

                        val stationText = if (report.stationName != null && report.stationDistanceKm != null) {
                            "${report.source.label} • ${report.stationName} (${TideFormatters.formatDistance(report.stationDistanceKm)}) • $timezoneBadgeLabel"
                        } else {
                            "${report.source.label} • ${TideFormatters.formatCoords(report.location.latitude, report.location.longitude)} • $timezoneBadgeLabel"
                        }
                        Text(
                            text = stationText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB5D6E6),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TideCurveCard(
    report: TideReport,
    tideUnit: TideUnit,
    windowHours: Int,
    timeZone: TimeZone,
    timezoneBadgeLabel: String,
    onChangeWindow: (Int) -> Unit,
    onOpenTimezonePicker: () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tide Prediction Graph",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${report.datumLabel} (${tideUnit.shortLabel}) • $timezoneBadgeLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onOpenTimezonePicker() }
                            .padding(vertical = 2.dp),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(24, 48).forEach { hours ->
                        FilterChip(
                            selected = windowHours == hours,
                            onClick = { onChangeWindow(hours) },
                            label = { Text("${hours}h") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                            modifier = Modifier.testTag("window_chip_${hours}h"),
                        )
                    }
                }
            }

            TideWaveChart(
                curvePoints = report.curvePoints,
                extrema = report.extrema,
                currentTimeMillis = report.currentTimeMillis,
                unit = tideUnit,
                windowHours = windowHours,
                timeZone = timeZone,
            )
        }
    }
}

@Composable
private fun TideExtremumRow(
    extremum: TideExtremum,
    unit: TideUnit,
    timeZone: TimeZone,
    nowMillis: Long,
) {
    val extended = LocalTideExtendedColors.current
    val isHigh = extremum.type == TideEventType.HIGH
    val badgeColor = if (isHigh) extended.highTideBadge else extended.lowTideBadge
    val isPast = extremum.timestampEpochMillis < nowMillis
    val formattedDate = TideFormatters.formatDateInZone(extremum.timestampEpochMillis, timeZone)
    val formattedTime = TideFormatters.formatTimeInZone(extremum.timestampEpochMillis, timeZone)

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isPast) 0.dp else 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = badgeColor.copy(alpha = 0.16f),
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isHigh) {
                                Icons.AutoMirrored.Rounded.TrendingUp
                            } else {
                                Icons.AutoMirrored.Rounded.TrendingDown
                            },
                            contentDescription = if (isHigh) "High tide" else "Low tide",
                            tint = badgeColor,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                Column {
                    Text(
                        text = if (isHigh) "High Tide" else "Low Tide",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "$formattedDate • $formattedTime",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = TideFormatters.formatHeight(extremum.heightMeters, unit),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = badgeColor,
                )
                Text(
                    text = if (isPast) "Passed" else "In ${TideFormatters.formatCountdown(extremum.timestampEpochMillis, nowMillis)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MarineConditionsGrid(
    conditions: MarineConditions,
    unit: TideUnit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TelemetryTile(
                icon = Icons.Rounded.Waves,
                label = "Wave Height",
                value = conditions.waveHeightMeters?.let { TideFormatters.formatHeightCompact(it, unit) } ?: "—",
                subtext = TideFormatters.formatDirectionCompass(conditions.waveDirectionDegrees),
                modifier = Modifier.weight(1f),
            )
            TelemetryTile(
                icon = Icons.Rounded.Timer,
                label = "Wave Period",
                value = conditions.wavePeriodSeconds?.let { "${it.toInt()} sec" } ?: "—",
                subtext = "Dominant interval",
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TelemetryTile(
                icon = Icons.Rounded.Water,
                label = "Primary Swell",
                value = conditions.swellHeightMeters?.let { TideFormatters.formatHeightCompact(it, unit) } ?: "—",
                subtext = "Offshore swell",
                modifier = Modifier.weight(1f),
            )
            TelemetryTile(
                icon = Icons.Rounded.Thermostat,
                label = "Water Temp",
                value = TideFormatters.formatTemp(conditions.seaSurfaceTempCelsius, unit),
                subtext = "Sea surface",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TelemetryTile(
    icon: ImageVector,
    label: String,
    value: String,
    subtext: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtext.isNotBlank()) {
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun NearbyStationCard(
    station: NearbyStation,
    isCurrentStation: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (isCurrentStation) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = 2.dp,
        border = if (isCurrentStation) {
            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        modifier = Modifier
            .width(210.dp)
            .testTag("nearby_station_${station.id}"),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Anchor,
                    contentDescription = "NOAA Station",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "NOAA #${station.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = station.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(station.state, TideFormatters.formatDistance(station.distanceKm))
                    .joinToString(" • "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun OnboardingLocationCard(
    featuredBeaches: List<BeachLocation>,
    isLocatingGps: Boolean,
    onUseGpsClick: () -> Unit,
    onOpenSearchClick: () -> Unit,
    onSelectBeach: (BeachLocation) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = DeepOcean),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.img_coastal_hero),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = 0.45f,
                    modifier = Modifier.matchParentSize(),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    AbyssNavy.copy(alpha = 0.72f),
                                    DeepOcean.copy(alpha = 0.92f),
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = "Live Coastal Tides Anywhere",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                    )
                    Text(
                        text = "Use your device GPS to lock onto the nearest shoreline station, or search any beach worldwide.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFC5E1EE),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = onUseGpsClick,
                            enabled = !isLocatingGps,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ReefTeal,
                                contentColor = AbyssNavy,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("onboarding_gps_button"),
                        ) {
                            Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isLocatingGps) "Locating…" else "Use GPS")
                        }
                        OutlinedButton(
                            onClick = onOpenSearchClick,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("onboarding_search_button"),
                        ) {
                            Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Search Beach")
                        }
                    }
                }
            }
        }

        SectionHeader(
            title = "Popular Coastal Spots",
            subtitle = "Tap any beach to load live tide telemetry",
        )

        featuredBeaches.forEach { beach ->
            Surface(
                onClick = { onSelectBeach(beach) },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("featured_beach_${beach.id}"),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Explore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column {
                            Text(
                                text = beach.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = beach.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        text = "View",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun InlandLocationCard(
    state: TideUiState.InlandLocation,
    featuredBeaches: List<BeachLocation>,
    onJumpToStation: (NearbyStation) -> Unit,
    onOpenSearchClick: () -> Unit,
    onSelectBeach: (BeachLocation) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Inland Location Detected",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.nearestStation?.let { nearest ->
                        Button(
                            onClick = { onJumpToStation(nearest) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text("Nearest: ${nearest.name}")
                        }
                    }
                    OutlinedButton(
                        onClick = onOpenSearchClick,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text("Search Coastline")
                    }
                }
            }
        }

        SectionHeader(
            title = "Jump to a Coastal Beach",
            subtitle = "Instant live tide stations",
        )
        featuredBeaches.take(4).forEach { beach ->
            Surface(
                onClick = { onSelectBeach(beach) },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(beach.name, style = MaterialTheme.typography.titleMedium)
                        Text(beach.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Rounded.Explore, contentDescription = "Open beach", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
