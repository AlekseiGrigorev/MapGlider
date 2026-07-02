package com.aleksvgrig.mapglider.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.maps.android.compose.MapType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class JoystickPosition {
    LEFT, CENTER, RIGHT
}

class SettingsRepository(private val context: Context) {
    private val mapTypeKey = stringPreferencesKey("map_type")
    private val joystickPositionKey = stringPreferencesKey("joystick_position")

    val mapTypeFlow: Flow<MapType> = context.dataStore.data
        .map { preferences ->
            val typeName = preferences[mapTypeKey] ?: MapType.SATELLITE.name
            try {
                MapType.valueOf(typeName)
            } catch (_: Exception) {
                MapType.SATELLITE
            }
        }

    val joystickPositionFlow: Flow<JoystickPosition> = context.dataStore.data
        .map { preferences ->
            val positionName = preferences[joystickPositionKey] ?: JoystickPosition.CENTER.name
            try {
                JoystickPosition.valueOf(positionName)
            } catch (_: Exception) {
                JoystickPosition.CENTER
            }
        }

    suspend fun saveMapType(mapType: MapType) {
        context.dataStore.edit { preferences ->
            preferences[mapTypeKey] = mapType.name
        }
    }

    suspend fun saveJoystickPosition(position: JoystickPosition) {
        context.dataStore.edit { preferences ->
            preferences[joystickPositionKey] = position.name
        }
    }
}
