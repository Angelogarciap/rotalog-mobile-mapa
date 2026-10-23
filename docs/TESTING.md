# Testes por seção

Os testes unitários (`app/src/test`) e instrumentados (`app/src/androidTest`) usam os mesmos pacotes por seção, abaixo de `com.rotalog.delivery`. Filtre pelo pacote para executar todos os testes da seção, incluindo novas classes, sem manter listas manuais.

| Seção | Sufixo do pacote | Unitários | Instrumentados |
| --- | --- | --- | --- |
| Login | `login` | `LoginViewModelTest` | `LoginFlowTest` |
| Rotas e paradas | `routes` | `RouteRulesTest` | `RouteFlowTest`, `RouteReviewTest`, `StopDetailsTest` |
| Etapas e recebimento | `loading` | `LoadingRulesTest`, `DepartureStateTest` | `LoadingFlowTest`, `LegacyDepartureRedirectTest` |
| Histórico | `history` | `HistoryRulesTest` | `HistoryFlowTest`, `HistoryConsultationTest` |
| Perfil e controles demonstrativos | `profile` | `ProfileRulesTest` | `ProfileFlowTest`, `ProfileReviewTest` |

## Durante a mudança

Execute somente os testes da seção alterada e das dependências ou fluxos relacionados. Exemplos para Perfil:

```bash
# Regras e estado, sem emulador
./gradlew :app:testDebugUnitTest --tests 'com.rotalog.delivery.profile.*'

# Interface, integração Android e capturas do Perfil, com emulador/dispositivo
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package=com.rotalog.delivery.profile
```

Para Login, Rotas, Histórico ou Etapas/recebimento, substitua `profile` pelo sufixo da tabela **nos dois comandos**. O Gradle ainda pode compilar fontes compartilhadas; os filtros limitam a execução dos testes.

Para uma iteração menor, filtre uma classe ou método:

```bash
./gradlew :app:testDebugUnitTest \
  --tests 'com.rotalog.delivery.login.LoginViewModelTest'
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rotalog.delivery.profile.ProfileFlowTest
# Um método: acrescente #nomeDoMetodo ao nome da classe instrumentada.
```

Filtros de unitários (`--tests`) e de instrumentados (`package`/`class`) são independentes. Não deixe filtros ativos na validação final. Não altere a configuração padrão para excluir seções. Se uma seção nova ainda não tiver testes de um dos níveis, registre a ausência em vez de interpretar “nenhum teste encontrado” como aprovação.

## Ao finalizar

Depois das iterações red → green → refactor nas suítes afetadas, execute a regressão completa uma vez, sem filtros, antes de entregar ou fazer commit:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

Repita as suítes necessárias somente se houver novas alterações, falhas ou dúvidas não resolvidas. Mudanças compartilhadas (tema, navegação, modelos, repositórios) exigem as seções afetadas durante o desenvolvimento. Os testes instrumentados devem continuar cobrindo os fluxos de integração necessários: abrir uma aba ou explorar a demonstração para preparar um teste não executa a suíte inteira daquela seção.

Execute um pacote ou classe por invocação: a tentativa de listar vários retornou somente o primeiro conjunto nesta revisão. Confira as classes no XML antes de contabilizar a cobertura.

Nesta revisão, as invocações instrumentadas usaram `--no-configuration-cache` e a lista de classes dos XMLs foi conferida para assegurar a aplicação do filtro solicitado; as execuções finais não usam filtros.

Use os requisitos de ambiente do README. Para interface/fluxo, valide as seções afetadas com KVM, nos dois temas, em tela pequena e fonte 200%; teclado quando aplicável. Relatórios ficam em `app/build/reports/tests/testDebugUnitTest/` e `app/build/reports/androidTests/connected/debug/`. Os relatórios padrão são substituídos pela execução seguinte: copie evidências relevantes para `app/build/review/` antes de executar outro filtro.

## Organização da cobertura

- Login cobre identificador vazio/alfanumérico/numérico/telefone, indisponibilidade sem sessão, entrada demonstrativa separada, restauração e revisão com teclado.
- Rotas conserva aceite, mapa/lista, seleção, painel, volumes/progresso e ações externas. Dados demonstrativos e tema são preparados diretamente pelos repositórios/preferências nos testes dessa seção.
- Etapas/recebimento cobre navegação entre três etapas, declaração global, divergências, nova conferência, recebimento, partida, migração e restauração; a partida permanece aqui como integração do fluxo de carregamento.
- Histórico tem suas próprias verificações do registro concluído, progresso e isolamento da rota atual, além da revisão nos dois temas.
- Perfil concentra temas, expansão, rolagem, restauração, cenários, reinício e envio simulado. As asserções operacionais de independência dos controles continuam nessa suíte.

A antiga revisão que visitava várias abas foi dividida em `RouteReviewTest` e `HistoryFlowTest`, `HistoryConsultationTest`. As capturas completas do Perfil já são cobertas por `ProfileReviewTest`. Nenhuma verificação operacional foi removida para acelerar a execução; o comando padrão continua executando todas as seções.
