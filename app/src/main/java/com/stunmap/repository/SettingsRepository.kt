package com.stunmap.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val API_KEY = stringPreferencesKey("ipinfo_api_key")
        val SHOW_NON_CANDIDATES = booleanPreferencesKey("show_non_candidates")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
    }

    val apiKeyFlow: Flow<String?> = context.dataStore.data.map { it[Keys.API_KEY] }
    val showNonCandidatesFlow: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.SHOW_NON_CANDIDATES] ?: false }
    val darkModeFlow: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.DARK_MODE] ?: true }

    suspend fun getApiKey(): String? = apiKeyFlow.first()

    suspend fun setApiKey(key: String) {
        context.dataStore.edit { it[Keys.API_KEY] = key }
    }

    suspend fun setShowNonCandidates(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_NON_CANDIDATES] = show }
    }

    suspend fun setDarkMode(dark: Boolean) {
        context.dataStore.edit { it[Keys.DARK_MODE] = dark }
    }
}
