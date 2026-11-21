package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.BeachLocation
import com.example.data.model.TideUnit
import com.example.data.model.TimezoneMode
import com.example.data.model.TimezonePreference
import com.example.data.remote.NetworkModule
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tidepulse_prefs")

class UserPreferencesRepository(private val context: Context) {
    private val moshi = NetworkModule.sharedMoshi()
    private val beachAdapter = moshi.adapter(BeachLocation::class.java)
    private val beachListType = Types.newParameterizedType(List::class.java, BeachLocation::class.java)
    private val beachListAdapter = moshi.adapter<List<BeachLocation>>(beachListType)

    private object Keys {
        val TIDE_UNIT = stringPreferencesKey("tide_unit")
        val SAVED_BEACHES = stringPreferencesKey("saved_beaches")
        val LAST_BEACH = stringPreferencesKey("last_beach")
        val TIMEZONE_MODE = stringPreferencesKey("timezone_mode")
        val CUSTOM_TIMEZONE_ID = stringPreferencesKey("custom_timezone_id")
    }

    val tideUnitFlow: Flow<TideUnit> = context.dataStore.data.map { prefs ->
        when (prefs[Keys.TIDE_UNIT]) {
            TideUnit.METERS.name -> TideUnit.METERS
            else -> TideUnit.FEET
        }
    }

    val savedBeachesFlow: Flow<List<BeachLocation>> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.SAVED_BEACHES] ?: return@map emptyList()
        runCatching { beachListAdapter.fromJson(raw) }.getOrNull().orEmpty()
    }

    val lastBeachFlow: Flow<BeachLocation?> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.LAST_BEACH] ?: return@map null
        runCatching { beachAdapter.fromJson(raw) }.getOrNull()
    }

    val timezonePreferenceFlow: Flow<TimezonePreference> = context.dataStore.data.map { prefs ->
        val modeName = prefs[Keys.TIMEZONE_MODE]
        val mode = TimezoneMode.entries.firstOrNull { it.name == modeName } ?: TimezoneMode.BEACH_LOCAL
        val customId = prefs[Keys.CUSTOM_TIMEZONE_ID]
        TimezonePreference(mode = mode, customZoneId = customId)
    }

    suspend fun setTideUnit(unit: TideUnit) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TIDE_UNIT] = unit.name
        }
    }

    suspend fun setTimezonePreference(pref: TimezonePreference) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TIMEZONE_MODE] = pref.mode.name
            if (pref.customZoneId != null) {
                prefs[Keys.CUSTOM_TIMEZONE_ID] = pref.customZoneId
            } else {
                prefs.remove(Keys.CUSTOM_TIMEZONE_ID)
            }
        }
    }

    suspend fun setLastBeach(beach: BeachLocation) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_BEACH] = beachAdapter.toJson(beach)
        }
    }

    suspend fun toggleSavedBeach(beach: BeachLocation) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.SAVED_BEACHES]
                ?.let { runCatching { beachListAdapter.fromJson(it) }.getOrNull() }
                .orEmpty()
                .toMutableList()
            val existingIndex = current.indexOfFirst {
                it.id == beach.id ||
                    (it.name == beach.name && kotlin.math.abs(it.latitude - beach.latitude) < 0.01)
            }
            if (existingIndex >= 0) {
                current.removeAt(existingIndex)
            } else {
                current.add(0, beach.copy(isGps = false))
            }
            prefs[Keys.SAVED_BEACHES] = beachListAdapter.toJson(current)
        }
    }

    suspend fun removeSavedBeach(beachId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.SAVED_BEACHES]
                ?.let { runCatching { beachListAdapter.fromJson(it) }.getOrNull() }
                .orEmpty()
                .filterNot { it.id == beachId }
            prefs[Keys.SAVED_BEACHES] = beachListAdapter.toJson(current)
        }
    }
}
