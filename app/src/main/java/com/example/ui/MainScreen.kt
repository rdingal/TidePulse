package com.example.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.TideUiState
import com.example.ui.components.TideFormatters
import com.example.ui.components.TimezonePickerSheet
import com.example.ui.screens.ExploreSearchScreen
import com.example.ui.screens.SavedBeachesScreen
import com.example.ui.screens.TideDashboardScreen

const val ROUTE_TIDES = "tides_dashboard"
const val ROUTE_EXPLORE = "explore_search"
const val ROUTE_FAVORITES = "favorites_beaches"

private data class NavDestinationItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

private val navDestinations = listOf(
    NavDestinationItem(
        route = ROUTE_TIDES,
        label = "Tides",
        selectedIcon = Icons.Rounded.Waves,
        unselectedIcon = Icons.Outlined.Waves,
        testTag = "nav_tab_tides",
    ),
    NavDestinationItem(
        route = ROUTE_EXPLORE,
        label = "Explore",
        selectedIcon = Icons.Rounded.Explore,
        unselectedIcon = Icons.Outlined.Explore,
        testTag = "nav_tab_explore",
    ),
    NavDestinationItem(
        route = ROUTE_FAVORITES,
        label = "Favorites",
        selectedIcon = Icons.Rounded.Favorite,
        unselectedIcon = Icons.Outlined.FavoriteBorder,
        testTag = "nav_tab_favorites",
    ),
)

@Composable
fun MainScreen(
    viewModel: TideViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tideUnit by viewModel.tideUnit.collectAsStateWithLifecycle()
    val savedBeaches by viewModel.savedBeaches.collectAsStateWithLifecycle()
    val timezonePreference by viewModel.timezonePreference.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val searchError by viewModel.searchError.collectAsStateWithLifecycle()
    val isLocatingGps by viewModel.isLocatingGps.collectAsStateWithLifecycle()
    val hasLocationPermission by viewModel.hasLocationPermission.collectAsStateWithLifecycle()
    val curveWindowHours by viewModel.curveWindowHours.collectAsStateWithLifecycle()
    val gpsMessage by viewModel.gpsMessage.collectAsStateWithLifecycle()

    var showTimezoneSheet by remember { mutableStateOf(false) }

    val activeReport = (uiState as? TideUiState.Success)?.report
    val (_, activeTimezoneLabel) = remember(
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

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_TIDES

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        viewModel.onPermissionResult(granted)
        if (granted) {
            navigateToTab(ROUTE_TIDES)
        }
    }

    fun triggerGpsRequest() {
        if (hasLocationPermission) {
            viewModel.fetchCurrentGpsTides()
            navigateToTab(ROUTE_TIDES)
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                )
            )
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        navDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label,
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                    ) {
                        navDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label,
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag),
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_TIDES,
                    modifier = Modifier.weight(1f),
                ) {
                    composable(ROUTE_TIDES) {
                        TideDashboardScreen(
                            uiState = uiState,
                            tideUnit = tideUnit,
                            timezonePreference = timezonePreference,
                            curveWindowHours = curveWindowHours,
                            savedBeaches = savedBeaches,
                            featuredBeaches = viewModel.featuredBeaches,
                            isLocatingGps = isLocatingGps,
                            gpsMessage = gpsMessage,
                            onUseGpsClick = ::triggerGpsRequest,
                            onOpenSearchClick = { navigateToTab(ROUTE_EXPLORE) },
                            onOpenFavoritesClick = { navigateToTab(ROUTE_FAVORITES) },
                            onOpenTimezonePicker = { showTimezoneSheet = true },
                            onSelectBeach = { beach -> viewModel.selectLocation(beach) },
                            onSelectNearbyStation = { station -> viewModel.selectNearbyStation(station) },
                            onToggleSaveBeach = { beach -> viewModel.toggleSavedBeach(beach) },
                            onChangeUnit = { unit -> viewModel.setTideUnit(unit) },
                            onChangeCurveWindow = { hours -> viewModel.setCurveWindowHours(hours) },
                            onRefreshClick = { viewModel.refreshCurrent() },
                            onDismissGpsMessage = { viewModel.dismissGpsMessage() },
                        )
                    }

                    composable(ROUTE_EXPLORE) {
                        ExploreSearchScreen(
                            searchQuery = searchQuery,
                            searchResults = searchResults,
                            isSearching = isSearching,
                            searchError = searchError,
                            featuredBeaches = viewModel.featuredBeaches,
                            savedBeaches = savedBeaches,
                            isLocatingGps = isLocatingGps,
                            onQueryChange = { viewModel.updateSearchQuery(it) },
                            onClearQuery = { viewModel.clearSearch() },
                            onUseGpsClick = ::triggerGpsRequest,
                            onSelectLocation = { beach ->
                                viewModel.selectLocation(beach)
                                navigateToTab(ROUTE_TIDES)
                            },
                            onToggleFavorite = { beach -> viewModel.toggleSavedBeach(beach) },
                        )
                    }

                    composable(ROUTE_FAVORITES) {
                        SavedBeachesScreen(
                            savedBeaches = savedBeaches,
                            tideUnit = tideUnit,
                            timezonePreference = timezonePreference,
                            activeTimezoneLabel = activeTimezoneLabel,
                            onSelectBeach = { beach ->
                                viewModel.selectLocation(beach)
                                navigateToTab(ROUTE_TIDES)
                            },
                            onRemoveBeach = { id -> viewModel.removeSavedBeach(id) },
                            onOpenExplore = { navigateToTab(ROUTE_EXPLORE) },
                            onOpenTimezonePicker = { showTimezoneSheet = true },
                            onChangeUnit = { unit -> viewModel.setTideUnit(unit) },
                        )
                    }
                }
            }
        }
    }

    if (showTimezoneSheet) {
        TimezonePickerSheet(
            currentPreference = timezonePreference,
            beachTimezoneLabel = activeReport?.timezoneLabel,
            onSelectPreference = { pref -> viewModel.setTimezonePreference(pref) },
            onDismiss = { showTimezoneSheet = false },
        )
    }
}
