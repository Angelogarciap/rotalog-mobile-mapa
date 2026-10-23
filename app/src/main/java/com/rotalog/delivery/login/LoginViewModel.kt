package com.rotalog.delivery.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LoginUiState(
    val identifier: String = "",
    val identifierError: String? = null,
    val message: String? = null,
    val exploringDemo: Boolean = false,
)

/** Local form only. Submitting an identifier never creates an authenticated session. */
class LoginViewModel(private val savedState: SavedStateHandle) : ViewModel() {
    private val mutableState = MutableStateFlow(LoginUiState(
        identifier = savedState["loginIdentifier"] ?: "",
        exploringDemo = savedState["exploringDemo"] ?: false,
    ))
    val uiState = mutableState.asStateFlow()

    fun setIdentifier(value: String) {
        savedState["loginIdentifier"] = value
        mutableState.update { it.copy(identifier = value, identifierError = null, message = null) }
    }

    fun submit() {
        mutableState.update {
            val error = if (it.identifier.trim().isEmpty()) "Informe seu identificador, matrícula ou telefone." else null
            it.copy(identifierError = error, message = if (error == null)
                "Acesso indisponível nesta versão. Nenhum dado foi enviado." else null)
        }
    }

    fun exploreDemo() {
        savedState["exploringDemo"] = true
        mutableState.update { LoginUiState(identifier = it.identifier, exploringDemo = true) }
    }
}
