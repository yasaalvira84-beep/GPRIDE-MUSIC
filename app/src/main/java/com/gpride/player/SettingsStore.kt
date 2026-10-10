package com.gpride.player

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class VisualizerPrefs(
    val enabled: Boolean = true,
    val style: VisualizerStyle = VisualizerStyle.Spectrum,
    val sensitivity: Int = 70,
    val fps: Int = 60,
    val colorIndex: Int = 0,
)

class SettingsStore(private val context: Context) {
    private val historyKey = booleanPreferencesKey("history_enabled")
    private val visEnabledKey = booleanPreferencesKey("vis_enabled")
    private val visStyleKey = intPreferencesKey("vis_style")
    private val visSensitivityKey = intPreferencesKey("vis_sensitivity")
    private val visFpsKey = intPreferencesKey("vis_fps")
    private val visColorKey = intPreferencesKey("vis_color")

    val historyEnabled: Flow<Boolean> = context.dataStore.data.map { it[historyKey] ?: true }

    suspend fun setHistoryEnabled(enabled: Boolean) {
        context.dataStore.edit { it[historyKey] = enabled }
    }

    val visualizer: Flow<VisualizerPrefs> = context.dataStore.data.map { p ->
        val defaults = VisualizerPrefs()
        VisualizerPrefs(
            enabled = p[visEnabledKey] ?: defaults.enabled,
            style = VisualizerStyle.entries.getOrElse(p[visStyleKey] ?: 0) { defaults.style },
            sensitivity = (p[visSensitivityKey] ?: defaults.sensitivity).coerceIn(0, 100),
            fps = (p[visFpsKey] ?: defaults.fps).coerceIn(15, 60),
            colorIndex = (p[visColorKey] ?: defaults.colorIndex).coerceIn(0, VisualizerPalettes.lastIndex),
        )
    }

    suspend fun setVisualizer(prefs: VisualizerPrefs) {
        context.dataStore.edit {
            it[visEnabledKey] = prefs.enabled
            it[visStyleKey] = prefs.style.ordinal
            it[visSensitivityKey] = prefs.sensitivity
            it[visFpsKey] = prefs.fps
            it[visColorKey] = prefs.colorIndex
        }
    }
}
