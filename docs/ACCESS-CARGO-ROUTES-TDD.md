# Etapas e carga: evidências da frente Rotas

Implementado no worktree `rotalog-ui-rotas`, base `de457d3`, sem commit. Versão preservada: 1.2 / código 3. Não houve novas dependências.

## Ciclos executados

Os logs ficam localmente em `app/build/review/access-cargo/routes-evidence/logs/` e não são versionados.

- `cargoAndDepartureDoNotRequireLegacyPreparation`: red comportamental com `checkCargo` rejeitado por ausência do preparo; green após remover o preparo dos requisitos de conferência, recebimento e partida. Campos e assinatura serializável de Route preservados; fixtures novas não simulam preparo.
- `globalConfirmationIsAtomicAndRequiresAcceptance`: red com rejeição do comando global após aceite; green com atualização atômica de todos os IDs do manifesto ou conjunto vazio. Não recebe nem inicia automaticamente.
- `DepartureStateTest`: três falhas comportamentais no red (sem avanço, marcação antiga preservada indevidamente, rascunho não aberto). Green com etapas no SavedStateHandle, migração versão 2 e rascunho separado do estado de envio.
- `delayedFailureFromPreviousScenarioDoesNotPolluteTheNewOne`: red com erro de operação anterior aparecendo no cenário novo; green protegendo também os catches pela geração do cenário.
- `legacyDestinationsValidateRouteAndOpenOnlyAnAvailableStep`: red com destino válido rejeitado; green com validação centralizada de routeId e consulta da etapa disponível. Navegação remove destinos legados do histórico.
- `acceptanceAutomaticallyOpensCompactReceiptWithOneGlobalCheckbox`: red instrumentado por ausência de “Mercadoria” após o aceite. Green com oito mercadorias visíveis sem rolagem inicial em 360 × 640 dp e fonte 100%, um checkbox global e partida separada. O teste também cobre cancelamento e revisão pelos dois mecanismos de Voltar.

## Verificações da frente

- Build debug aprovado isoladamente.
- Unitários das seções loading, routes, history e profile aprovados após implementação e refatoração.
- `LoadingFlowTest`: 3 testes aprovados em 360 × 640 dp / fonte 100%, em AVD com KVM. Exercitam fluxo completo, divergência/restauração, cancelamento, resolução/nova conferência e capturas em ambos os temas.
- `routes` instrumentado aprovado em execução separada (conferir contagem no XML preservado nos logs); inclui números disponíveis e futuros bloqueados.
- Revisão posterior acrescenta recriação em Recebimento e Início, retorno pelo Android no rascunho e cobertura explícita de números/etapas futuras em RouteFlowTest. A validação final integrada deve executar esses acréscimos sem cache de configuração e conferir as classes no XML.
- Tentativa com dois pacotes separados por vírgula retornou apenas os três testes de LoadingFlowTest; não é contabilizada como execução da suíte Routes. Usar invocações separadas e `--no-configuration-cache` ao alterar filtros.

## Cobertura e diferenças deliberadas

O checklist não é mais uma ação do fluxo ativo. Sua cobertura operacional foi substituída por declaração global, regras sem preparo, migração dos snapshots e recebimento/partida independentes. Os campos legados permanecem disponíveis para desserialização, sem serem preenchidos em novas demonstrações. Divergência e resolução limpam toda a declaração global; manifesto mantém quantidades e ordem. Os seletores começam vazios, descrição é opcional e limitada a 500 caracteres, rascunho só limpa após sucesso.

O manifesto usa tabela compacta e a origem extensa abre em consulta secundária. O indicador diferencia avanço confirmado, etapa operacional atual e consulta selecionada. Popup usa Surface opaca, limite de 420 dp / 85% da altura, margens e dim de 55%; conteúdo e ações rolam juntos. A revisão final de fonte 200%, teclado e limites de largura, lint e regressão completa pertence à integração e não foi antecipadamente declarada aprovada nesta frente.


## Regressão de concorrência na integração

A revisão reproduziu uma operação suspensa que retorna depois de reiniciar o mesmo cenário: bloquear somente o feedback pela geração não impedia que o comando antigo alterasse o novo modelo com o mesmo `routeId`.

- **Red real:** `resettingSameRouteCancelsPendingAcceptanceBeforeRepositoryMutation` falhou porque o aceite atrasado mudou a rota reiniciada de ASSIGNED para ACCEPTED. O teste `resettingSameRouteCancelsPendingCargoReceiptAndIssueBeforeRepositoryMutation` falhou no primeiro caso porque a conferência atrasada marcou a carga da nova operação. Ambos usam `CompletableDeferred.await()` antes de delegar ao repositório, sem falha de compilação ou ambiente. Evidências: `logs/reset-race-red.log` e `logs/reset-race-red.xml` em `app/build/review/access-cargo/`.
- **Green:** as operações possuem um `Job` cancelado antes de reiniciar ou trocar o cenário; a invalidação libera os estados de operação e preserva a proteção por geração para feedback e `finally`. Inclui aceite restaurado, comandos comuns e salvamento do formulário. O teste de matriz confirma também recebimento e divergência sem mutação tardia. O cancelamento usa o contrato cooperativo das funções suspensas; nenhum envio remoto é iniciado nesta demonstração.
- **Refactor:** invalidação centralizada usada pelo reset explícito e pelo observador de substituição externa da rota. Flags de uma geração antiga não liberam operação nova. Unitários de loading/routes/history/profile executados novamente, sem cache de configuração; conferir resultado em `logs/reset-race-refactor.log`.
