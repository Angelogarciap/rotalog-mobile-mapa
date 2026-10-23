package com.rotalog.delivery.data

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rotalog.delivery.model.ThemeMode
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsStore by preferencesDataStore(
    name = "settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

class DataStoreSettingsRepository(context: Context) : SettingsRepository {
    private val store = context.applicationContext.settingsStore
    private val themeKey = stringPreferencesKey("theme")
    override val theme = store.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { prefs -> ThemeMode.entries.find { it.name == prefs[themeKey] } ?: ThemeMode.SYSTEM }

    override suspend fun setTheme(mode: ThemeMode) {
        store.edit { it[themeKey] = mode.name }
    }
}
