package ru.company.izhs_planner.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "izhs_preferences")

class PreferencesManager(private val context: Context) {
    private val dataStore = context.dataStore

    val themeMode: Flow<ThemeMode> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val themeName = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
            ThemeMode.valueOf(themeName)
        }

    val isDisclaimerAccepted: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.DISCLAIMER_ACCEPTED] ?: false
        }

    val messagesToday: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val savedDate = preferences[PreferencesKeys.MESSAGES_DATE] ?: 0L
            val today = System.currentTimeMillis() / 86400000L
            if (savedDate != today) {
                0
            } else {
                preferences[PreferencesKeys.MESSAGES_TODAY] ?: 0
            }
        }

    val isPremiumActive: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.IS_PREMIUM_ACTIVE] ?: false
        }

    val projectCount: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.PROJECT_COUNT] ?: 0
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun setDisclaimerAccepted(accepted: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DISCLAIMER_ACCEPTED] = accepted
        }
    }

    suspend fun incrementMessagesToday() {
        dataStore.edit { preferences ->
            val savedDate = preferences[PreferencesKeys.MESSAGES_DATE] ?: 0L
            val today = System.currentTimeMillis() / 86400000L
            if (savedDate != today) {
                preferences[PreferencesKeys.MESSAGES_DATE] = today
                preferences[PreferencesKeys.MESSAGES_TODAY] = 1
            } else {
                val current = preferences[PreferencesKeys.MESSAGES_TODAY] ?: 0
                preferences[PreferencesKeys.MESSAGES_TODAY] = current + 1
            }
        }
    }

    suspend fun setPremiumActive(active: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_PREMIUM_ACTIVE] = active
        }
    }

    suspend fun setProjectCount(count: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PROJECT_COUNT] = count
        }
    }

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DISCLAIMER_ACCEPTED = booleanPreferencesKey("disclaimer_accepted")
        val MESSAGES_DATE = longPreferencesKey("messages_date")
        val MESSAGES_TODAY = intPreferencesKey("messages_today")
        val IS_PREMIUM_ACTIVE = booleanPreferencesKey("is_premium_active")
        val PROJECT_COUNT = intPreferencesKey("project_count")
    }
}

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}