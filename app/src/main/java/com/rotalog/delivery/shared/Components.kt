package com.rotalog.delivery.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rotalog.delivery.ui.theme.RotaLogSpacing
import com.rotalog.delivery.ui.theme.RotalogTheme

@Composable
fun DemoBanner(text: String = "Demonstração • dados fictícios") {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(RotaLogSpacing.card),
            verticalArrangement = Arrangement.spacedBy(RotaLogSpacing.medium),
            content = content,
        )
    }
}

@Composable
fun StatusBadge(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
fun PrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = RotaLogSpacing.primaryAction),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun PageTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ComponentPreview(dark: Boolean) {
    RotalogTheme(darkTheme = dark) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.padding(RotaLogSpacing.page),
                verticalArrangement = Arrangement.spacedBy(RotaLogSpacing.card),
            ) {
                PageTitle("Suas rotas", "Tudo pronto para uma boa entrega.")
                DemoBanner()
                SectionCard {
                    StatusBadge("Aguardando aceite")
                    Text("Rota #RT-1024", style = MaterialTheme.typography.titleLarge)
                    Text("8 paradas • Manaus, AM", style = MaterialTheme.typography.bodyMedium)
                    PrimaryAction("Aceitar rota", onClick = {})
                }
            }
        }
    }
}

@Preview(name = "Componentes · Claro", showBackground = true, widthDp = 360)
@Composable
private fun LightComponentsPreview() = ComponentPreview(dark = false)

@Preview(name = "Componentes · Escuro", showBackground = true, widthDp = 360)
@Composable
private fun DarkComponentsPreview() = ComponentPreview(dark = true)

@Preview(name = "Componentes · Fonte 200%", showBackground = true, widthDp = 360, fontScale = 2f)
@Composable
private fun LargeFontComponentsPreview() = ComponentPreview(dark = false)
