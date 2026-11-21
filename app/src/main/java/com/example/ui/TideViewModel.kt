package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.location.LocationProvider
import com.example.data.model.BeachLocation
import com.example.data.model.LocationSearchResult
import com.example.data.model.NearbyStation
import com.example.data.model.TideUiState
import com.example.data.model.TideUnit
import com.example.data.model.TimezonePreference
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.TideRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TideViewModel(application: Application) : AndroidViewModel(application) {
    private val locationProvider = LocationProvider(application)
    private val preferences = UserPreferencesRepository(application)
    private val repository = TideRepository()

    private val _uiState = MutableStateFlow<TideUiState>(TideUiState.AwaitingLocation)
    val uiState: StateFlow<TideUiState> = _uiState.asStateFlow()

    private val _isLocatingGps = MutableStateFlow(false)
    val isLocatingGps: StateFlow<Boolean> = _isLocatingGps.asStateFlow()

    private val _hasLocationPermission = MutableStateFlow(locationProvider.hasLocationPermission())
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<LocationSearchResult>>(emptyList())
    val searchResults: StateFlow<List<LocationSearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    private val _curveWindowHours = MutableStateFlow(24)
    val curveWindowHours: StateFlow<Int> = _curveWindowHours.asStateFlow()

    private val _gpsMessage = MutableStateFlow<String?>(null)
    val gpsMessage: StateFlow<String?> = _gpsMessage.asStateFlow()

    val tideUnit: StateFlow<TideUnit> = preferences.tideUnitFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TideUnit.FEET,
    )

    val savedBeaches: StateFlow<List<BeachLocation>> = preferences.savedBeachesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val timezonePreference: StateFlow<TimezonePreference> = preferences.timezonePreferenceFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TimezonePreference(),
    )

    val featuredBeaches: List<BeachLocation> = listOf(
        BeachLocation(
            id = "preset_santa_monica",
            name = "Santa Monica Pier",
            subtitle = "California, United States",
            latitude = 34.0086,
            longitude = -118.4980,
            noaaStationId = "9410840",
        ),
        BeachLocation(
            id = "preset_miami_beach",
            name = "Miami Beach",
            subtitle = "Florida, United States",
            latitude = 25.7686,
            longitude = -80.1322,
        ),
        BeachLocation(
            id = "preset_waikiki",
            name = "Waikiki Beach",
            subtitle = "Honolulu, Hawaii",
            latitude = 21.2767,
            longitude = -157.8294,
            noaaStationId = "1612340",
        ),
        BeachLocation(
            id = "preset_san_francisco",
            name = "Crissy Field / Golden Gate",
            subtitle = "San Francisco, California",
            latitude = 37.8065,
            longitude = -122.4659,
            noaaStationId = "9414290",
        ),
        BeachLocation(
            id = "preset_brighton",
            name = "Brighton Beach",
            subtitle = "East Sussex, United Kingdom",
            latitude = 50.8190,
            longitude = -0.1364,
        ),
        BeachLocation(
            id = "preset_bondi",
            name = "Bondi Beach",
            subtitle = "Sydney, Australia",
            latitude = -33.8915,
            longitude = 151.2767,
        ),
    )

    private var currentLocation: BeachLocation? = null
    private var searchJob: Job? = null
    private var loadJob: Job? = null

    init {
        bootstrapInitialLocation()
    }

    private fun bootstrapInitialLocation() {
        viewModelScope.launch {
            val granted = locationProvider.hasLocationPermission()
            _hasLocationPermission.value = granted
            if (granted) {
                fetchCurrentGpsTides()
            } else {
                val lastBeach = preferences.lastBeachFlow.first()
                if (lastBeach != null) {
                    selectLocation(lastBeach)
                } else {
                    _uiState.value = TideUiState.AwaitingLocation
                }
            }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _hasLocationPermission.value = granted
        if (granted) {
            _gpsMessage.value = null
            fetchCurrentGpsTides()
        } else {
            _gpsMessage.value = "Location access declined. You can still search any beach or coastal city below."
        }
    }

    fun dismissGpsMessage() {
        _gpsMessage.value = null
    }

    fun fetchCurrentGpsTides() {
        if (!locationProvider.hasLocationPermission()) {
            _hasLocationPermission.value = false
            return
        }
        _hasLocationPermission.value = true
        viewModelScope.launch {
            _isLocatingGps.value = true
            _gpsMessage.value = null
            val locationResult = locationProvider.getCurrentBeachLocation()
            _isLocatingGps.value = false
            locationResult.fold(
                onSuccess = { gpsBeach ->
                    selectLocation(gpsBeach)
                },
                onFailure = { err ->
                    _gpsMessage.value = err.localizedMessage ?: "Could not read GPS location."
                    if (_uiState.value is TideUiState.AwaitingLocation) {
                        val last = preferences.lastBeachFlow.first()
                        if (last != null) selectLocation(last)
                    }
                }
            )
        }
    }

    fun selectLocation(location: BeachLocation) {
        currentLocation = location
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = TideUiState.Loading
            preferences.setLastBeach(location)
            _uiState.value = repository.fetchTideReport(location)
        }
    }

    fun selectNearbyStation(station: NearbyStation) {
        val stationBeach = BeachLocation(
            id = "noaa_${station.id}",
            name = station.name,
            subtitle = listOfNotNull(station.state, "NOAA Station #${station.id}").joinToString(" • "),
            latitude = station.latitude,
            longitude = station.longitude,
            noaaStationId = station.id,
        )
        selectLocation(stationBeach)
    }

    fun refreshCurrent() {
        val loc = currentLocation
        if (loc != null) {
            if (loc.isGps && locationProvider.hasLocationPermission()) {
                fetchCurrentGpsTides()
            } else {
                selectLocation(loc)
            }
        } else if (locationProvider.hasLocationPermission()) {
            fetchCurrentGpsTides()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _searchError.value = null
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            delay(320)
            val result = repository.searchLocations(query)
            _isSearching.value = false
            result.fold(
                onSuccess = { items ->
                    _searchResults.value = items
                },
                onFailure = { err ->
                    _searchError.value = err.localizedMessage ?: "Search failed"
                }
            )
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _searchError.value = null
        _isSearching.value = false
    }

    fun setTideUnit(unit: TideUnit) {
        viewModelScope.launch {
            preferences.setTideUnit(unit)
        }
    }

    fun setTimezonePreference(pref: TimezonePreference) {
        viewModelScope.launch {
            preferences.setTimezonePreference(pref)
        }
    }

    fun setCurveWindowHours(hours: Int) {
        _curveWindowHours.value = hours
    }

    fun toggleSavedBeach(location: BeachLocation) {
        viewModelScope.launch {
            preferences.toggleSavedBeach(location)
        }
    }

    fun removeSavedBeach(beachId: String) {
        viewModelScope.launch {
            preferences.removeSavedBeach(beachId)
        }
    }

    fun isBeachSaved(location: BeachLocation, saved: List<BeachLocation>): Boolean {
        return saved.any {
            it.id == location.id ||
                (it.name == location.name && kotlin.math.abs(it.latitude - location.latitude) < 0.01)
        }
    }
}
