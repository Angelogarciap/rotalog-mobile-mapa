package com.rotalog.delivery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotalog.delivery.data.SettingsRepository
import com.rotalog.delivery.model.ThemeMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val theme = repository.theme.map<ThemeMode, ThemeMode?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val failure = MutableStateFlow<String?>(null)
    val error = failure.asStateFlow()
    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch {
            try { repository.setTheme(mode) }
            catch (_: IOException) { failure.value = "Não foi possível salvar o tema. Tente novamente." }
        }
    }
    fun clearError() { failure.value = null }
}
