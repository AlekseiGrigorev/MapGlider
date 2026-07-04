package com.aleksvgrig.mapglider.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
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
    private val tiltKey = floatPreferencesKey("tilt")
    private val joystickSizeKey = floatPreferencesKey("joystick_size")

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

    val tiltFlow: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[tiltKey] ?: 45f
        }

    val joystickSizeFlow: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[joystickSizeKey] ?: 1.0f
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

    suspend fun saveTilt(tilt: Float) {
        context.dataStore.edit { preferences ->
            preferences[tiltKey] = tilt
        }
    }

    suspend fun saveJoystickSize(size: Float) {
        context.dataStore.edit { preferences ->
            preferences[joystickSizeKey] = size
        }
    }
}
