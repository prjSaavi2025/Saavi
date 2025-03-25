package com.saavi.saavi


import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension to create DataStore
val Context.dataStore by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    private val LANGUAGE_KEY = stringPreferencesKey("selected_language")

    // Save selected language
    suspend fun saveLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language
        }
    }

    // Get saved language (default to "Malayalam")
    val selectedLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LANGUAGE_KEY] ?: "Malayalam"
    }
}
