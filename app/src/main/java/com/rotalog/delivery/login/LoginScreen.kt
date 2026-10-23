package com.rotalog.delivery.login

import com.rotalog.delivery.shared.SectionCard
import com.rotalog.delivery.shared.AppIcons
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    state: LoginUiState,
    onIdentifierChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onExploreDemo: () -> Unit,
) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val submit = { keyboard?.hide(); focus.clearFocus(); onSubmit() }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.safeDrawingPadding().imePadding(), contentAlignment = Alignment.TopCenter) {
            val minimumHeight = maxHeight
            Column(
                Modifier.widthIn(max = 420.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                    .heightIn(min = minimumHeight).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                LoginBrand()
                Spacer(Modifier.weight(1f))
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(AppIcons.Truck, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Acesso do motorista", style = MaterialTheme.typography.titleLarge)
                    }
                    Text("Informe o acesso fornecido pela sua operação.", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SectionCard {
                        state.message?.let { message ->
                            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) {
                                Text(message, Modifier.padding(12.dp).semantics { liveRegion = LiveRegionMode.Polite },
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        OutlinedTextField(
                            value = state.identifier, onValueChange = onIdentifierChange,
                            modifier = Modifier.fillMaxWidth().testTag("login-identifier"),
                            label = { Text("Identificador / Matrícula ou Telefone") },
                            placeholder = { Text("DRV-9082 ou (92) 99999-9999") },
                            singleLine = true, shape = RoundedCornerShape(12.dp),
                            isError = state.identifierError != null,
                            supportingText = state.identifierError?.let { error -> { Text(error) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { submit() }),
                        )
                        Button(onClick = submit, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(12.dp)) {
                            Text("Entrar no RotaLog")
                        }
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Acesso para entregadores cadastrados.", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Versão demonstrativa • autenticação não conectada", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onExploreDemo, modifier = Modifier.heightIn(min = 48.dp)) { Text("Explorar demonstração") }
                }
            }
        }
    }
}

@Composable
private fun LoginBrand() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Icon(AppIcons.Truck, null, Modifier.size(26.dp))
            }
        }
        Column {
            Text("RotaLog", style = MaterialTheme.typography.titleMedium)
            Text("Entregador", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
