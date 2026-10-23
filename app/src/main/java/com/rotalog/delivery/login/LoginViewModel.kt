package com.rotalog.delivery.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val message: String? = null,
    val exploringDemo: Boolean = false,
)

/** Local form only. A valid form never creates an authenticated session. */
class LoginViewModel(private val savedState: SavedStateHandle) : ViewModel() {
    private val mutableState = MutableStateFlow(LoginUiState(
        email = savedState["loginEmail"] ?: "",
        exploringDemo = savedState["exploringDemo"] ?: false,
    ))
    val uiState = mutableState.asStateFlow()

    fun setEmail(value: String) {
        savedState["loginEmail"] = value
        mutableState.update { it.copy(email = value, emailError = null, message = null) }
    }
    fun setPassword(value: String) {
        mutableState.update { it.copy(password = value, passwordError = null, message = null) }
    }
    fun togglePassword() { mutableState.update { it.copy(passwordVisible = !it.passwordVisible) } }
    fun submit() {
        mutableState.update {
            val emailError = if (EMAIL.matches(it.email.trim())) null else "Informe um e-mail válido."
            val passwordError = if (it.password.isBlank()) "Digite sua senha." else null
            it.copy(emailError = emailError, passwordError = passwordError,
                message = if (emailError == null && passwordError == null)
                    "Autenticação indisponível nesta versão. Nenhum dado foi enviado." else null)
        }
    }
    fun recoverPassword() {
        mutableState.update { it.copy(message = "A recuperação de senha ainda não está conectada. Solicite ajuda à sua operação.") }
    }
    fun exploreDemo() {
        savedState["exploringDemo"] = true
        mutableState.update { LoginUiState(email = it.email, exploringDemo = true) }
    }
    private companion object {
        val EMAIL = Regex("^[A-Za-z0-9.!#%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+$")
    }
}
