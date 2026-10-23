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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rotalog.delivery.model.*
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull

@Composable
fun RoutesScreen(
    state: RoutesUiState,
    onAccept: () -> Unit,
    onSelectStop: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onShowStops: (String) -> Unit,
    onNavigate: (Stop) -> Unit,
    onCall: (Stop) -> Unit,
    onNextStop: () -> Unit,
    onPreparation: (String) -> Unit,
    onReceipt: (String) -> Unit,
    onStart: () -> Unit,
    routeMap: RouteMap = DemonstrationRouteMap
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
        state.route.status == RouteStatus.ACTIVE -> ActiveRoute(state, onSelectStop, onExpandedChange, onShowStops, onNavigate, onCall, onNextStop, routeMap)
        else -> AssignedRoute(state, onAccept, onShowStops, onPreparation, onReceipt, onStart)
    }
}

@Composable
private fun AssignedRoute(state: RoutesUiState, onAccept: () -> Unit, onShowStops: (String) -> Unit, onPreparation: (String) -> Unit, onReceipt: (String) -> Unit, onStart: () -> Unit) {
    val route = state.route ?: return
    var confirm by rememberSaveable { mutableStateOf(false) }
    var confirmStart by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DemoNotice()
        Heading("Sua rota atribuída")
        Text("Confira os dados e confirme o aceite para continuar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
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
        Stage("2", "Preparação para saída", if (route.prepared) "Confirmada" else "Pendente", route.prepared)
        Stage("3", "Recebimento da carga", if (route.cargoReceived) "Confirmado" else "Pendente", route.cargoReceived)
        if (route.status == RouteStatus.ASSIGNED) {
            Button(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Aceitar rota") }
        } else {
            Button(onClick = { onPreparation(route.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(if (route.prepared) "Revisar checklist" else "Preparar saída")
            }
            OutlinedButton(onClick = { onReceipt(route.id) }, enabled = route.prepared || route.cargoReceived, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(if (route.cargoReceived) "Revisar carga recebida" else "Conferir carga no CD")
            }
            if (route.canStart) {
                Button(onClick = { confirmStart = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Iniciar rota") }
            } else {
                Text("Partida bloqueada até confirmar o checklist e o recebimento da carga.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        SyncNotice(route.syncStatus)
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(12.dp))
    }
    if (confirmStart) AlertDialog(
        onDismissRequest = { confirmStart = false }, title = { Text("Iniciar rota demonstrativa?") },
        text = { Text("A carga foi recebida e o checklist confirmado. O mapa abrirá na primeira entrega pendente. Esta demonstração não ativa GPS nem envia dados ao servidor.") },
        confirmButton = { TextButton(onClick = { confirmStart = false; onStart() }) { Text("Iniciar agora") } },
        dismissButton = { TextButton(onClick = { confirmStart = false }) { Text("Cancelar") } }
    )
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
    onNextStop: () -> Unit,
    routeMap: RouteMap
) {
    val route = state.route ?: return
    val selected = state.selectedStop ?: return
    val expandedChange by rememberUpdatedState(onExpandedChange)
    val sheetState = rememberStandardBottomSheetState(
        initialValue = if (state.expanded) SheetValue.Expanded else SheetValue.PartiallyExpanded,
        skipHiddenState = true
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
    val density = LocalDensity.current
    val detailsScroll = key(selected.id) { rememberScrollState() }
    var movingFromState by remember { mutableStateOf(false) }
    LaunchedEffect(state.expanded) {
        movingFromState = true
        try {
            if (!state.expanded) detailsScroll.scrollTo(0)
            if (state.expanded) sheetState.expand() else sheetState.partialExpand()
        } finally { movingFromState = false }
    }
    LaunchedEffect(sheetState) {
        // Only settled gestures may update the saved state. Intermediate programmatic
        // positions must not overwrite a newer selection or expansion request.
        snapshotFlow {
            if (movingFromState || sheetState.isAnimationRunning || sheetState.currentValue != sheetState.targetValue) null
            else sheetState.currentValue
        }.filterNotNull().distinctUntilChanged().drop(1).collect {
            expandedChange(it == SheetValue.Expanded)
        }
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(route.code, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${route.completedStops} de ${route.stops.size} concluídas", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { onShowStops(route.id) }, modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Lista de paradas" }) { Text("Paradas") }
            }
            LinearProgressIndicator(progress = { route.progress }, modifier = Modifier.fillMaxWidth())
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val minimumMapHeight = if (density.fontScale > 1.3f) 180.dp else 136.dp
            val maxSheetHeight = (maxHeight - minimumMapHeight - 48.dp).coerceAtLeast(120.dp)
            val peekHeight = (210.dp * density.fontScale.coerceAtMost(1.4f)).coerceAtMost(maxSheetHeight)
            val visibleHeight by remember(sheetState, maxHeight, density) {
                derivedStateOf { with(density) { runCatching { sheetState.requireOffset().toDp() }.getOrDefault(maxHeight - peekHeight) }.coerceAtLeast(104.dp) }
            }
            BottomSheetScaffold(
                scaffoldState = scaffoldState,
                sheetPeekHeight = peekHeight,
                sheetDragHandle = { BottomSheetDefaults.DragHandle(modifier = Modifier.testTag("route-sheet-handle")) },
                sheetContainerColor = MaterialTheme.colorScheme.surface,
                sheetContent = {
                    Column(Modifier.fillMaxWidth().height(maxSheetHeight).padding(horizontal = 20.dp)) {
                        Button(onClick = { onExpandedChange(!state.expanded) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                            Text(if (state.expanded) "Recolher detalhes" else "Ver detalhes")
                        }
                        Column(Modifier.weight(1f).verticalScroll(detailsScroll).padding(top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(if (selected.id == route.nextStop?.id) "PRÓXIMA PARADA" else "PARADA SELECIONADA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text("${selected.sequence}. ${selected.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("${selected.type.label} • ${selected.status.label}")
                            Text(selected.address, style = MaterialTheme.typography.bodyMedium)
                            Text("Janela: ${selected.window}", style = MaterialTheme.typography.bodyMedium)
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
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                routeMap.Content(route.stops, selected.id, route.nextStop?.id, visibleHeight, state.expanded, onSelectStop, Modifier.fillMaxSize())
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
