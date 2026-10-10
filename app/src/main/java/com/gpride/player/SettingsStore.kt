package com.gpride.player

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
    private val lyricsOnlineKey = booleanPreferencesKey("lyrics_online")
    private val eqEnabledKey = booleanPreferencesKey("eq_enabled")
    private val eqPresetKey = intPreferencesKey("eq_preset")
    private val eqCustomKey = stringPreferencesKey("eq_custom")
    private val eqBassKey = intPreferencesKey("eq_bass")
    private val fadeSecKey = intPreferencesKey("fade_sec")
    private val skipSilenceKey = booleanPreferencesKey("skip_silence")

    val historyEnabled: Flow<Boolean> = context.dataStore.data.map { it[historyKey] ?: true }

    suspend fun setHistoryEnabled(enabled: Boolean) {
        context.dataStore.edit { it[historyKey] = enabled }
    }

    /** Cari lirik otomatis lewat internet (mengirim judul/artis/album/durasi ke LRCLIB). */
    val lyricsOnline: Flow<Boolean> = context.dataStore.data.map { it[lyricsOnlineKey] ?: true }

    suspend fun setLyricsOnline(enabled: Boolean) {
        context.dataStore.edit { it[lyricsOnlineKey] = enabled }
    }

    val audio: Flow<AudioPrefs> = context.dataStore.data.map { p ->
        val d = AudioPrefs()
        AudioPrefs(
            eqEnabled = p[eqEnabledKey] ?: d.eqEnabled,
            eqPreset = (p[eqPresetKey] ?: d.eqPreset).coerceIn(-1, EqPresets.lastIndex),
            eqCustomMb = (p[eqCustomKey] ?: "").split(",").mapNotNull { it.trim().toIntOrNull() },
            bassBoost = (p[eqBassKey] ?: d.bassBoost).coerceIn(0, 1000),
            fadeSec = (p[fadeSecKey] ?: d.fadeSec).coerceIn(0, 8),
            skipSilence = p[skipSilenceKey] ?: d.skipSilence,
        )
    }

    suspend fun setAudio(prefs: AudioPrefs) {
        context.dataStore.edit {
            it[eqEnabledKey] = prefs.eqEnabled
            it[eqPresetKey] = prefs.eqPreset
            it[eqCustomKey] = prefs.eqCustomMb.joinToString(",")
            it[eqBassKey] = prefs.bassBoost
            it[fadeSecKey] = prefs.fadeSec
            it[skipSilenceKey] = prefs.skipSilence
        }
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
