package com.rotalog.delivery.login

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.*
import org.junit.Test

/** Supplementary boundary and state checks for the flow driven by instrumented TDD. */
class LoginViewModelTest {
    @Test fun malformedEmailsAndBlankPasswordsAreRejected() {
        for (email in listOf("", "nome@", "a@b", "a b@exemplo.com", "a@-dominio.com", "a@b..com")) {
            val vm = LoginViewModel(SavedStateHandle())
            vm.setEmail(email)
            vm.setPassword("   ")
            vm.submit()
            assertEquals(email, "Informe um e-mail válido.", vm.uiState.value.emailError)
            assertEquals("Digite sua senha.", vm.uiState.value.passwordError)
            assertNull(vm.uiState.value.message)
            assertFalse(vm.uiState.value.exploringDemo)
        }
    }
    @Test fun validFormWithSurroundingEmailSpacesNeverAuthenticates() {
        val vm = LoginViewModel(SavedStateHandle())
        vm.setEmail("  nome+operacao@sub.exemplo.com.br  ")
        vm.setPassword(" segredo ")
        repeat(2) { vm.submit() }
        assertNull(vm.uiState.value.emailError)
        assertNull(vm.uiState.value.passwordError)
        assertEquals(" segredo ", vm.uiState.value.password)
        assertEquals("Autenticação indisponível nesta versão. Nenhum dado foi enviado.", vm.uiState.value.message)
        assertFalse(vm.uiState.value.exploringDemo)
    }
    @Test fun editingClearsOnlyRelevantErrorAndFeedback() {
        val vm = LoginViewModel(SavedStateHandle())
        vm.submit()
        vm.setEmail("nome@exemplo.com")
        assertNull(vm.uiState.value.emailError)
        assertNotNull(vm.uiState.value.passwordError)
        vm.recoverPassword()
        vm.setPassword("segredo")
        assertNull(vm.uiState.value.passwordError)
        assertNull(vm.uiState.value.message)
    }
    @Test fun restoredStateExcludesPasswordAndDemoEntryClearsCredentials() {
        val saved = SavedStateHandle()
        val vm = LoginViewModel(saved)
        vm.setEmail("nome@exemplo.com")
        vm.setPassword("segredo")
        vm.togglePassword()
        val restored = LoginViewModel(saved)
        assertEquals("nome@exemplo.com", restored.uiState.value.email)
        assertEquals("", restored.uiState.value.password)
        assertFalse(restored.uiState.value.passwordVisible)
        assertEquals(setOf("loginEmail"), saved.keys())
        vm.exploreDemo()
        assertTrue(vm.uiState.value.exploringDemo)
        assertEquals("", vm.uiState.value.password)
        assertFalse(vm.uiState.value.passwordVisible)
        assertTrue(LoginViewModel(saved).uiState.value.exploringDemo)
    }
}
