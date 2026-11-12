package com.focus.tomato.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class DarkMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val workMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val longBreakInterval: Int = 4,
    val autoStartNext: Boolean = false,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val darkMode: DarkMode = DarkMode.SYSTEM
)

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val WORK = intPreferencesKey("work_minutes")
        val SHORT = intPreferencesKey("short_break_minutes")
        val LONG = intPreferencesKey("long_break_minutes")
        val INTERVAL = intPreferencesKey("long_break_interval")
        val AUTO_START = intPreferencesKey("auto_start_next")
        val SOUND = intPreferencesKey("sound_enabled")
        val VIBRATION = intPreferencesKey("vibration_enabled")
        val DARK = stringPreferencesKey("dark_mode")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { p ->
        AppSettings(
            workMinutes = p[Keys.WORK] ?: 25,
            shortBreakMinutes = p[Keys.SHORT] ?: 5,
            longBreakMinutes = p[Keys.LONG] ?: 15,
            longBreakInterval = p[Keys.INTERVAL] ?: 4,
            autoStartNext = (p[Keys.AUTO_START] ?: 0) == 1,
            soundEnabled = (p[Keys.SOUND] ?: 1) == 1,
            vibrationEnabled = (p[Keys.VIBRATION] ?: 1) == 1,
            darkMode = p[Keys.DARK]?.let { runCatching { DarkMode.valueOf(it) }.getOrNull() } ?: DarkMode.SYSTEM
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.settingsDataStore.edit { p ->
            val current = AppSettings(
                workMinutes = p[Keys.WORK] ?: 25,
                shortBreakMinutes = p[Keys.SHORT] ?: 5,
                longBreakMinutes = p[Keys.LONG] ?: 15,
                longBreakInterval = p[Keys.INTERVAL] ?: 4,
                autoStartNext = (p[Keys.AUTO_START] ?: 0) == 1,
                soundEnabled = (p[Keys.SOUND] ?: 1) == 1,
                vibrationEnabled = (p[Keys.VIBRATION] ?: 1) == 1,
                darkMode = p[Keys.DARK]?.let { runCatching { DarkMode.valueOf(it) }.getOrNull() } ?: DarkMode.SYSTEM
            )
            val next = transform(current)
            p[Keys.WORK] = next.workMinutes
            p[Keys.SHORT] = next.shortBreakMinutes
            p[Keys.LONG] = next.longBreakMinutes
            p[Keys.INTERVAL] = next.longBreakInterval
            p[Keys.AUTO_START] = if (next.autoStartNext) 1 else 0
            p[Keys.SOUND] = if (next.soundEnabled) 1 else 0
            p[Keys.VIBRATION] = if (next.vibrationEnabled) 1 else 0
            p[Keys.DARK] = next.darkMode.name
        }
    }
}
