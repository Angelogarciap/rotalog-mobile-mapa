package com.rotalog.delivery.login

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.*
import org.junit.Test

class LoginViewModelTest {
    @Test fun identifiersNeedNoPasswordOrEmailSyntax() {
        for (identifier in listOf("DRV-9082", "9082", "(92) 99999-9999", "  DRV-9082  ")) {
            val vm = LoginViewModel(SavedStateHandle())
            vm.setIdentifier(identifier)
            repeat(2) { vm.submit() }
            assertNull(vm.uiState.value.identifierError)
            assertEquals("Acesso indisponível nesta versão. Nenhum dado foi enviado.", vm.uiState.value.message)
            assertFalse(vm.uiState.value.exploringDemo)
        }
    }

    @Test fun emptyOrWhitespaceIdentifierIsRejectedWithoutCreatingSession() {
        for (identifier in listOf("", "   ", "\t\n")) {
            val vm = LoginViewModel(SavedStateHandle())
            vm.setIdentifier(identifier)
            vm.submit()
            assertEquals("Informe seu identificador, matrícula ou telefone.", vm.uiState.value.identifierError)
            assertNull(vm.uiState.value.message)
            assertFalse(vm.uiState.value.exploringDemo)
        }
    }

    @Test fun editingClearsErrorAndFeedback() {
        val vm = LoginViewModel(SavedStateHandle())
        vm.submit()
        vm.setIdentifier("9082")
        assertNull(vm.uiState.value.identifierError)
        vm.submit()
        assertNotNull(vm.uiState.value.message)
        vm.setIdentifier("DRV-9082")
        assertNull(vm.uiState.value.message)
    }

    @Test fun taskStateRestoresIdentifierAndSeparateDemoChoice() {
        val saved = SavedStateHandle()
        val vm = LoginViewModel(saved)
        vm.setIdentifier("(92) 99999-9999")
        val restored = LoginViewModel(saved)
        assertEquals("(92) 99999-9999", restored.uiState.value.identifier)
        assertEquals(setOf("loginIdentifier"), saved.keys())
        assertFalse(restored.uiState.value.exploringDemo)
        vm.exploreDemo()
        assertTrue(vm.uiState.value.exploringDemo)
        assertNull(vm.uiState.value.message)
        assertTrue(LoginViewModel(saved).uiState.value.exploringDemo)
    }

    @Test fun legacyEmailDoesNotBecomeAnIdentifier() {
        val vm = LoginViewModel(SavedStateHandle(mapOf("loginEmail" to "antigo@exemplo.com")))
        assertEquals("", vm.uiState.value.identifier)
        vm.submit()
        assertNotNull(vm.uiState.value.identifierError)
    }
}
