package com.rotalog.delivery.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rotalog.delivery.model.DemoScenario
import com.rotalog.delivery.model.SyncStatus
import com.rotalog.delivery.model.ThemeMode

@Composable
fun ProfileScreen(
    theme: ThemeMode,
    scenario: DemoScenario,
    syncStatus: SyncStatus,
    onThemeChange: (ThemeMode) -> Unit,
    onScenarioChange: (DemoScenario) -> Unit,
    onSyncStatusChange: (SyncStatus) -> Unit,
    onResetScenario: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Perfil", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Seu perfil e suas preferências.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Card(
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(modifier = Modifier.size(64.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("CE", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Text("Carlos Eduardo Mendes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Entregador • MOT-7832", color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider()
                Text("Veículo", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Renault Master • PHB-9A23", style = MaterialTheme.typography.bodyLarge)
                Text("Identidade demonstrativa", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        ProfileSection("Aparência", "A preferência de tema é salva neste aparelho.") {
            Column(Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { mode ->
                    ProfileChoice(mode.label, mode == theme) { onThemeChange(mode) }
                }
            }
        }
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ambiente de demonstração", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    "Dados fictícios, mapa sem GPS e estados de envio simulados. Os controles abaixo permitem explorar as telas.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        ProfileSection(
            "Cenário da rota",
            "O cenário ativo é um exemplo independente. Aceitar a rota atribuída mantém recebimento e preparação pendentes."
        ) {
            Column(Modifier.selectableGroup()) {
                DemoScenario.entries.forEach { option ->
                    ProfileChoice(option.label, option == scenario) { onScenarioChange(option) }
                }
            }
            Text(
                "Os dados da operação ficam na memória desta demonstração. Somente o tema é mantido após fechar o aplicativo.",
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onResetScenario,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp).heightIn(min = 56.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Reiniciar cenário atual") }
        }
        ProfileSection(
            "Estado de envio • simulado",
            "A escolha é manual. Não há envio ao servidor nem confirmação automática nesta entrega."
        ) {
            Column(Modifier.selectableGroup()) {
                SyncStatus.entries.forEach { option ->
                    ProfileChoice(option.label, option == syncStatus) { onSyncStatusChange(option) }
                }
            }
        }
        Text(
            "RotaLog Entregador • primeira entrega nativa",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileSection(title: String, description: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        content()
    }
}

@Composable
private fun ProfileChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}
