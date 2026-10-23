package com.rotalog.delivery.login

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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotalog.delivery.shared.AppIcons
import com.rotalog.delivery.ui.theme.RotaLogColors

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
        Box(Modifier.safeDrawingPadding().imePadding(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 420.dp).fillMaxWidth()
                    .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp),
            ) {
                LoginBrand()
                Spacer(Modifier.height(32.dp))
                Text("Acesso do motorista", style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(8.dp))
                Text("Informe o acesso fornecido pela sua operação.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))
                state.message?.let { message ->
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                        Text(message, Modifier.padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite },
                            style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(16.dp))
                }
                val identifierLabel = "Identificador / Matrícula ou Telefone"
                Text(identifierLabel, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics { hideFromAccessibility() })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.identifier, onValueChange = onIdentifierChange,
                    modifier = Modifier.fillMaxWidth().testTag("login-identifier")
                        .semantics { contentDescription = identifierLabel },
                    placeholder = { Text("DRV-9082 ou (92) 99999-9999") },
                    singleLine = true, shape = MaterialTheme.shapes.medium,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    isError = state.identifierError != null,
                    supportingText = state.identifierError?.let { error -> { Text(error) } },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = submit, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RotaLogColors.Blue,
                        contentColor = RotaLogColors.LightSurface),
                ) {
                    Text("Entrar no RotaLog", style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(12.dp))
                Text("Acesso para entregadores cadastrados.", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onExploreDemo, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.small) {
                    Text("Explorar demonstração", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(4.dp))
                Text("Versão demonstrativa • autenticação não conectada", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LoginBrand() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(shape = MaterialTheme.shapes.medium, color = RotaLogColors.Blue,
            contentColor = RotaLogColors.LightSurface) {
            Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                Icon(AppIcons.Truck, null, Modifier.size(32.dp))
            }
        }
        Column {
            Text("RotaLog", style = MaterialTheme.typography.titleLarge.copy(fontSize = 26.sp, lineHeight = 32.sp))
            Text("Entregador", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
