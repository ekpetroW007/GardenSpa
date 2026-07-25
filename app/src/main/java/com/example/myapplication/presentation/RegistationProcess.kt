package com.example.myapplication.presentation


import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class PreferencesManager(context: Context) {
    private val dataStore = context.dataStore

    companion object {
        val IS_REGISTERED = booleanPreferencesKey("is_registered")
        val USER_LOGIN = stringPreferencesKey("user_login")
    }

    suspend fun setRegistered(isRegistered: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_REGISTERED] = isRegistered
        }
    }

    suspend fun setUserLogin(login: String) {
        dataStore.edit { preferences ->
            preferences[USER_LOGIN] = login
        }
    }

    val isRegistered: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[IS_REGISTERED] ?: false
        }

    val userLogin: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[USER_LOGIN] ?: "Гость"
        }
}