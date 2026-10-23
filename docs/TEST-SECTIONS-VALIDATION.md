# Validação da organização dos testes por seção

## Mudança

O refinamento anterior do Perfil foi salvo primeiro no commit `d65ccd7`. Esta entrega reorganiza testes e documentação; não altera fontes de produção, dependências, configuração Gradle ou a execução padrão da CI.

Testes unitários e instrumentados foram movidos para os pacotes `login`, `routes`, `loading`, `history` e `profile`, sob `com.rotalog.delivery`. Os comandos filtrados e o catálogo ficam em [TESTING.md](TESTING.md). `AGENTS.md` e README orientam rodar as seções afetadas durante o trabalho e toda a regressão sem filtros ao finalizar.

- `DeliveryFlowTest` passa a ser `routes.RouteFlowTest`; a verificação de temas/cenários/envio foi movida intacta para `profile.ProfileFlowTest`.
- Controles demonstrativos (troca/reinício de cenário e envio manual) ficam em `profile.ProfileRulesTest`, incluindo a proteção do rascunho ao selecionar novamente o cenário.
- As duas asserções do histórico, antes dentro do teste de quantidades da rota, passam para `history.HistoryRulesTest`. Isso cria um teste unitário independente: total de 16 para 17, preservando as asserções anteriores.
- O passeio de revisão por várias abas foi dividido em `routes.RouteReviewTest` e `history.HistoryFlowTest`. As capturas completas do Perfil continuam em `profile.ProfileReviewTest`. O Histórico acrescenta asserções explícitas de registro concluído e ausência da rota atual. O total instrumentado passa de 17 para 18.
- Rotas e carregamento preparam tema/cenário diretamente nos serviços existentes, evitando testar os controles do Perfil como requisito de preparação. Integrações que fazem parte do comportamento continuam: entrada na demonstração, seleção entre mapa/lista, retorno à aba, partida após carregamento e independência operacional dos controles do Perfil.

## Estratégia

É uma refatoração das suítes de um aplicativo já testado, sem novo comportamento de produção ou correção de bug. A base anterior estava verde (16 unitários / 17 instrumentados, registrada em PROFILE-VALIDATION.md). Não se apresenta uma falha artificial como etapa red. Foram mantidas as regras e asserções existentes, com execução das seções afetadas após a reorganização e regressão completa ao final.

## Ambiente e execução

JDK `/opt/android-studio/jbr`, AVD `Pixel_10a`, API 37, fonte 100%, tamanho/densidade originais. `emulator -accel-check` confirmou KVM disponível (código 0). Requisitos completos continuam no README.

As execuções por seção combinaram:

```bash
./gradlew :app:testDebugUnitTest --tests 'com.rotalog.delivery.profile.*' \
  :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package=com.rotalog.delivery.profile
```

Também executadas com `history` e `routes` nos dois filtros. Os XMLs preservados mostram somente as classes do pacote selecionado; compilar as demais fontes não significa executar as demais suítes.

Logs e resultados em `app/build/review/test-sections/`, fora do Git. Relatórios padrão em `app/build/reports/`.

## Resultados por seção

| Filtro | Unitários aprovados | Instrumentados aprovados | Evidência local |
| --- | ---: | ---: | --- |
| `profile` | 3 | 6 | `profile.log`, `profile-results/` |
| `history` | 1 | 1 | `history.log`, `history-results/` |
| `routes` | 4 | 5 | `routes.log`, `routes-results/` |

Todos sem falhas ou testes ignorados. O inventário estático `method-inventory.json` confirmou a preservação dos métodos anteriores, exceto o passeio combinado substituído pelas revisões de Rotas e Histórico: 33 métodos antes, 35 depois (17 unitários e 18 instrumentados).

## Regressão final sem filtros

Executado uma vez, depois das verificações por seção e da organização final:

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew \
  :app:assembleDebug :app:testDebugUnitTest :app:lintDebug \
  :app:connectedDebugAndroidTest --console=plain
```

- Build debug aprovado.
- 17 unitários e 18 instrumentados aprovados, sem falhas, erros ou testes ignorados. Todas as cinco seções estão presentes nos relatórios finais.
- Lint aprovado: zero erros e 11 avisos anteriores sobre versões disponíveis.
- `git diff --check` e revisão do conteúdo staged aprovados. Arquivos locais de `.idea` e artefatos de build excluídos dos commits.
- Log `full.log` e cópias dos XMLs em `full-results/`.

Validação local no emulador com KVM; sem execução em aparelho físico ou CI remota. A matriz de tela pequena/fontes 100% e 200% da interface está registrada na entrega anterior; esta refatoração não muda a interface e não repetiu essa matriz. Versão do aplicativo permanece 1.2 / versionCode 3.
