package com.rotalog.delivery.routes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rotalog.delivery.model.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

@Composable
fun RoutesScreen(
    state: RoutesUiState,
    onAccept: () -> Unit,
    onSelectStop: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onShowStops: (String) -> Unit,
    onNavigate: (Stop) -> Unit,
    onCall: (Stop) -> Unit,
    onNextStop: () -> Unit
) {
    when {
        state.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        state.route == null -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            DemoNotice()
            Heading("Nenhuma rota atribuída")
            Text("Quando a operação atribuir uma rota, ela aparecerá aqui. Por enquanto, você está em um cenário demonstrativo.")
            Text("Você pode explorar os outros cenários na aba Perfil.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        state.route.status == RouteStatus.ACTIVE -> ActiveRoute(state, onSelectStop, onExpandedChange, onShowStops, onNavigate, onCall, onNextStop)
        else -> AssignedRoute(state, onAccept, onShowStops)
    }
}

@Composable
private fun AssignedRoute(state: RoutesUiState, onAccept: () -> Unit, onShowStops: (String) -> Unit) {
    val route = state.route ?: return
    var confirm by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DemoNotice()
        Heading("Sua rota atribuída")
        Text("Confira os dados e confirme o aceite para continuar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(route.code, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(route.date)
                StatusText(route.status.label)
                Text("${route.stops.size} paradas • ${route.deliveryVolumes} volumes para entrega", style = MaterialTheme.typography.titleMedium)
                Text("${route.collectionVolumes} volumes previstos para coleta")
                HorizontalDivider()
                Text("A sequência é definida pela operação.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { onShowStops(route.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Ver lista de paradas") }
            }
        }
        Text("Antes de sair", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Stage("1", "Aceite da rota", if (route.status == RouteStatus.ACCEPTED) "Confirmado" else "Pendente", route.status == RouteStatus.ACCEPTED)
        Stage("2", "Recebimento da carga", if (route.cargoReceived) "Confirmado" else "Pendente • próxima etapa", route.cargoReceived)
        Stage("3", "Preparação para saída", if (route.prepared) "Confirmada" else "Pendente • próxima etapa", route.prepared)
        if (route.status == RouteStatus.ASSIGNED) {
            Button(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Aceitar rota") }
        } else {
            Text("Rota aceita. O recebimento da carga e a preparação continuam pendentes. Essas etapas estarão disponíveis na próxima entrega.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SyncNotice(route.syncStatus)
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(12.dp))
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Aceitar ${route.code}?") },
        text = { Text("O aceite confirma a atribuição desta rota. Você ainda precisará receber a carga e preparar a saída.") },
        confirmButton = { TextButton(onClick = { confirm = false; onAccept() }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Confirmar aceite") } },
        dismissButton = { TextButton(onClick = { confirm = false }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Voltar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveRoute(
    state: RoutesUiState,
    onSelectStop: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onShowStops: (String) -> Unit,
    onNavigate: (Stop) -> Unit,
    onCall: (Stop) -> Unit,
    onNextStop: () -> Unit
) {
    val route = state.route ?: return
    val selected = state.selectedStop ?: return
    val sheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded, skipHiddenState = true)
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
    val density = LocalDensity.current
    val detailsScroll = key(selected.id) { rememberScrollState() }
    LaunchedEffect(state.expanded) { if (!state.expanded) detailsScroll.scrollTo(0) }
    LaunchedEffect(state.expanded) { if (state.expanded) sheetState.expand() else sheetState.partialExpand() }
    LaunchedEffect(sheetState) {
        snapshotFlow { sheetState.currentValue }.distinctUntilChanged().drop(1).collect {
            onExpandedChange(it == SheetValue.Expanded)
        }
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(route.code, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                TextButton(onClick = { onShowStops(route.id) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Lista de paradas") }
            }
            Text("Demonstração • ${route.completedStops} de ${route.stops.size} paradas concluídas", style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(progress = { route.progress }, modifier = Modifier.fillMaxWidth())
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val maxSheetHeight = (maxHeight - 152.dp).coerceAtLeast(140.dp)
            val peekHeight = (210.dp * density.fontScale.coerceAtMost(1.4f)).coerceAtMost(maxSheetHeight)
            val visibleHeight by remember(sheetState, maxHeight, density) {
                derivedStateOf { with(density) { runCatching { sheetState.requireOffset().toDp() }.getOrDefault(maxHeight - peekHeight) }.coerceAtLeast(104.dp) }
            }
            BottomSheetScaffold(
                scaffoldState = scaffoldState,
                sheetPeekHeight = peekHeight,
                sheetContainerColor = MaterialTheme.colorScheme.surface,
                sheetContent = {
                    Column(Modifier.fillMaxWidth().heightIn(max = maxSheetHeight).verticalScroll(detailsScroll).padding(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(if (selected.id == route.nextStop?.id) "PRÓXIMA PARADA" else "PARADA SELECIONADA", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text("${selected.sequence}. ${selected.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${selected.type.label} • ${selected.status.label}")
                        Text(selected.address, style = MaterialTheme.typography.bodyMedium)
                        Text("Janela: ${selected.window}", style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = { onExpandedChange(!state.expanded) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                            Text(if (state.expanded) "Recolher detalhes" else "Ver detalhes")
                        }
                        if (selected.id != route.nextStop?.id && route.nextStop != null) {
                            OutlinedButton(onClick = onNextStop, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Voltar à próxima parada") }
                        }
                        HorizontalDivider()
                        StopDetails(selected, onNavigate, onCall)
                        HorizontalDivider()
                        Text("Carga da rota", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Coletados: ${route.collectedVolumes} / ${route.collectionVolumes} volumes\nEntregues: ${route.deliveredVolumes} / ${route.deliveryVolumes} volumes")
                        SyncNotice(route.syncStatus)
                        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                DemoRouteMap(route.stops, selected.id, route.nextStop?.id, visibleHeight, onSelectStop, Modifier.fillMaxSize(), expanded = state.expanded)
            }
        }
    }
}

@Composable
private fun StopDetails(stop: Stop, onNavigate: (Stop) -> Unit, onCall: (Stop) -> Unit) {
    Text("Detalhes da parada", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Detail("Endereço completo", stop.address)
    Detail("Referência", stop.reference)
    Detail("Janela de atendimento", stop.window)
    Detail("Contato", "${stop.contact}\n${stop.phone}")
    Button(onClick = { onNavigate(stop) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Navegar") }
    if (stop.phone.isNotBlank()) OutlinedButton(onClick = { onCall(stop) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Ligar para o contato") }
    Detail("Instruções", stop.instructions)
    Text("Produtos e quantidades", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    if (stop.items.isEmpty()) Text("Nenhum produto previsto nesta parada.")
    stop.items.forEach { item ->
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.description, fontWeight = FontWeight.SemiBold)
                Text("${item.quantity} ${item.unit}")
            }
        }
    }
    Text("${stop.volumes} volumes ${if (stop.type == StopType.COLLECTION) "para coleta" else if (stop.type == StopType.DELIVERY) "para entrega" else "previstos"}", fontWeight = FontWeight.SemiBold)
}

@Composable
fun StopsScreen(route: Route?, onSelectStop: (String) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("← Voltar para a rota") }
        Heading("Lista de paradas")
        DemoNotice()
        if (route == null) Text("Esta rota não está disponível.") else {
            Text("${route.code} • ${route.stops.size} paradas na ordem da operação")
            route.stops.sortedBy { it.sequence }.forEach { stop ->
                Card(onClick = { onSelectStop(stop.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${stop.sequence}. ${stop.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${stop.type.label} • ${stop.status.label}${if (stop.id == route.nextStop?.id) " • Próxima parada" else ""}", color = MaterialTheme.colorScheme.primary)
                        Text(stop.address)
                        Text("${stop.window} • ${stop.volumes} volumes")
                        Text("Selecionar parada →", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun Stage(number: String, title: String, subtitle: String, complete: Boolean) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (complete) "✓" else number, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Detail(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}

@Composable
private fun Heading(text: String) = Text(text, Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

@Composable
private fun DemoNotice() {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
        Text("Demonstração • dados fictícios", Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
private fun StatusText(text: String) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SyncNotice(status: SyncStatus) {
    Text("Envio simulado: ${status.label}. Altere a simulação no Perfil.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
