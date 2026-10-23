package com.rotalog.delivery.data

import com.rotalog.delivery.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory fixtures. Demo commands never represent server acknowledgements. */
class DemoRouteRepository : RouteRepository, DemoScenarioController {
    private val currentScenario = MutableStateFlow(DemoScenario.ASSIGNED)
    override val scenario = currentScenario.asStateFlow()
    private val currentRoute = MutableStateFlow<Route?>(DemoRoutes.assigned())
    override val route = currentRoute.asStateFlow()
    override val history = MutableStateFlow(DemoRoutes.history()).asStateFlow()

    override suspend fun acceptRoute(routeId: String): AcceptRouteResult {
        val route = currentRoute.value ?: return AcceptRouteResult.NotAvailable
        if (route.id != routeId) return AcceptRouteResult.NotAvailable
        if (route.status != RouteStatus.ASSIGNED) return AcceptRouteResult.AlreadyAccepted
        currentRoute.value = route.copy(status = RouteStatus.ACCEPTED, syncStatus = SyncStatus.LOCAL)
        return AcceptRouteResult.Accepted
    }

    override fun selectScenario(scenario: DemoScenario) {
        currentScenario.value = scenario
        resetScenario()
    }

    override fun resetScenario() {
        currentRoute.value = when (scenario.value) {
            DemoScenario.ASSIGNED -> DemoRoutes.assigned()
            DemoScenario.EMPTY -> null
            DemoScenario.ACTIVE -> DemoRoutes.active()
        }
    }

    override fun simulateSync(status: SyncStatus) {
        currentRoute.value = currentRoute.value?.copy(syncStatus = status)
    }
}

object DemoRoutes {
    private fun items(id: String, quantity: Int) = listOf(StopItem(id, "Mercadorias embaladas", "caixas", quantity))
    private fun stops(): List<Stop> = listOf(
        Stop("cd", 1, StopType.COLLECTION, StopStatus.PENDING, "CD Central Manaus",
            "Av. Rodrigo Otávio, 1000 - Distrito Industrial I, Manaus - AM", "Doca de expedição 2", "13:00 – 13:20",
            "Mariana • expedição", "+5592900000000", "Aguardar a equipe na doca de expedição. Conferir os volumes antes da saída.", items("carga-rota", 70)),
        Stop("rio-negro", 2, StopType.DELIVERY, StopStatus.PENDING, "Drogaria Rio Negro",
            "Av. Sete de Setembro, 800 - Centro, Manaus - AM", "Entrada de serviço lateral", "13:30 – 14:00",
            "Ana • responsável", "+5592900000000", "Solicitar o responsável pelo recebimento na entrada lateral.", items("higiene", 8)),
        Stop("amazonas", 3, StopType.DELIVERY, StopStatus.PENDING, "Supermercado Amazonas",
            "Av. Djalma Batista, 1420 - Chapada, Manaus - AM", "Portão B, pela Rua Rio Purus", "14:00 – 15:30",
            "Roberto (gerente)", "+5592900000000", "Acesso pelos fundos, Portão B (Rua Rio Purus). Apresentar nota fiscal e aguardar conferência na recepção de mercadorias.",
            listOf(StopItem("oleo", "Óleo", "caixas", 8), StopItem("arroz", "Arroz", "fardos", 4), StopItem("cafe", "Café", "caixas", 2))),
        Stop("ponta-negra", 4, StopType.DELIVERY, StopStatus.PENDING, "Distribuidora Ponta Negra",
            "Av. Coronel Teixeira, 520 - Ponta Negra, Manaus - AM", "Área de descarga sinalizada", "15:45 – 16:30",
            "João • recebimento", "+5592900000000", "Estacionar na área de descarga e aguardar a conferência.", items("limpeza", 20)),
        Stop("adrianopolis", 5, StopType.DELIVERY, StopStatus.PENDING, "Empório Adrianópolis",
            "R. Teresina, 310 - Adrianópolis, Manaus - AM", "Porta lateral do empório", "16:45 – 17:15",
            "Luiza • responsável", "+5592900000000", "Entregar pela porta lateral. Manter as caixas protegidas da chuva.", items("mercearia", 6)),
        Stop("parque-dez", 6, StopType.DELIVERY, StopStatus.PENDING, "Mercantil Parque Dez",
            "Av. Nilton Lins, 880 - Parque Dez, Manaus - AM", "Ao lado do estacionamento", "17:30 – 18:00",
            "Paulo • recebimento", "+5592900000000", "Usar a entrada de fornecedores ao lado do estacionamento.", items("alimentos", 12)),
        Stop("cidade-nova", 7, StopType.DELIVERY, StopStatus.PENDING, "Varejão Cidade Nova",
            "Av. Max Teixeira, 190 - Cidade Nova, Manaus - AM", "Portão de recebimento", "18:15 – 18:45",
            "Rita • responsável", "+5592900000000", "Aguardar a liberação do portão antes de descarregar.", items("bebidas", 10)),
        Stop("taruma", 8, StopType.RETURN, StopStatus.PENDING, "Retorno Garagem Tarumã",
            "Av. do Turismo, 400 - Tarumã, Manaus - AM", "Portaria da garagem", "19:15",
            "Equipe da base", "+5592900000000", "Apresentar-se à equipe da base para os procedimentos de retorno.", emptyList())
    )

    fun assigned() = Route("assigned-mao-042", "ROTA-MAO-042", "22/10/2026", RouteStatus.ASSIGNED, false, false, stops())
    fun active() = Route("demo-active-mao-042", "ROTA-MAO-042", "22/10/2026", RouteStatus.ACTIVE, true, true,
        stops().map { if (it.sequence <= 2) it.copy(status = StopStatus.COMPLETED) else it })

    fun history(): List<Route> = listOf(
        Route("history-mao-041", "ROTA-MAO-041", "21/10/2026", RouteStatus.COMPLETED, true, true,
            stops().map { it.copy(id = "previous-${it.id}", status = StopStatus.COMPLETED) }, SyncStatus.CONFIRMED)
    )
}
