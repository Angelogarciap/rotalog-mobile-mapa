# Acesso, etapas e recebimento — versão 1.2 / código 3

## Escopo

Implementação do plano aprovado a partir de `de457d3`, em três worktrees: integração (`feature/ux-acesso-recebimento`), Rotas (`feature/ux-etapas-carga`) e Login (`feature/ux-login-identificador`). As alterações locais preexistentes nos três arquivos de `.idea` permanecem no worktree original. A validação inicial foi concluída antes de commit, push ou merge. A integração posterior, autorizada pelo usuário, está registrada ao final deste documento.

O fluxo passa a Aceite → Recebimento → Início. O checklist de veículo deixa de participar das regras; seus campos serializados permanecem para compatibilidade. A etapa consultada é separada do progresso operacional. A declaração global substitui a conferência por produto; divergências exigem nova conferência global. O acesso usa identificador/matrícula/telefone e informa indisponibilidade sem enviar dados ou criar sessão.

Não foram adicionadas dependências, autenticação, GPS, notificações ao CD ou confirmações reais de servidor. Inter, paletas e temas são conservados.

## Referências e diferenças do Stitch

Referência: projeto `5674232400967196939`, telas de acesso `848c20e408664cd3bb7cadd88f41474a` e `00c89d2ca2ae4ce7a80d5392b43c8a22`, conforme plano aprovado. O projeto não abriu pela ferramenta web nesta sessão; não houve inspeção direta adicional dessas telas. A referência numerada descrita no plano orienta o indicador horizontal.

O acesso real indisponível e a entrada demonstrativa separada substituem a autenticação do protótipo. Indicadores de conexão, usuário ativo e pedidos de permissões ficam fora da mudança. Etapas revisáveis, recebimento global, resolução local e partida separada são adaptações operacionais deliberadas. O mapa demonstrativo e a camada existente dos detalhes da parada continuam após a partida.

## TDD e regressão

Falhas de compilação/configuração não são consideradas red comportamental. Evidências detalhadas: [Login](ACCESS-LOGIN-TDD.md) e [Rotas](ACCESS-CARGO-ROUTES-TDD.md). Os logs e XMLs locais ficam em `app/build/review/access-cargo/`, incluindo `login-evidence/`, `routes-evidence/` e `logs/`.

| Comportamento | Red observado antes da correção | Green / refactor |
| --- | --- | --- |
| Identificador sem senha | Código `DRV-9082` retornava `null` em vez do aviso de indisponibilidade | 5 unitários Login aprovados |
| Composição do acesso | `Acesso do motorista` ausente na interface | 4 instrumentados Login aprovados em cada fonte 100%/200% |
| Retirada dos bloqueios legados | Conferência rejeitada por falta de preparo | Regras permitem recebimento/partida sem checklist |
| Declaração global | Comando global rejeitado após aceite | Atualização atômica, sem receber ou iniciar implicitamente |
| Etapas, migração e rascunho | Sem avanço automático, marcação antiga mantida, rascunho não aberto | Estado salvo separado do progresso/envio e testes direcionados aprovados |
| Manifesto compacto | `Mercadoria` ausente após aceite | Oito produtos iniciais e um checkbox aprovados em 360 × 640 dp / 100% |
| Destinos antigos | Destino válido rejeitado | Validação centralizada de `routeId` e etapa disponível |
| Falha tardia após troca | Erro anterior aparecia no cenário novo | Feedback protegido pela geração |
| Operação suspensa após reinício | 2 testes alteravam a nova rota com mesmo identificador | Cancelamento do Job antes de reset/troca; 26 unitários afetados aprovados após refactor |
| Teclado no diálogo com fonte 200% | `Cancelar` não exibido após rolar; captura mostrava só o fundo e o teclado | Retirado recuo IME duplicado; janela redimensiona com o teclado e mantém as ações alcançáveis |
| Destino antigo sem rota | `obsoleteDestinationReturnsWhenEmptyScenarioFinishesLoading`: esperado 1 retorno, observado 0 | Efeito observa também carregamento; 2 instrumentados de redirecionamento aprovados |

O red de restauração sem rota foi executado após extrair, sem mudar seu comportamento, o redirecionamento duplicado dos dois destinos para `LegacyDepartureRedirect`. Depois da falha comportamental, a chave do efeito passou a incluir `loading` e o identificador solicitado. A seção loading foi repetida após a correção, com 5 instrumentados aprovados.

Casos complementares cobrem entrada vazia e formatos livres, revisão por números, etapas futuras bloqueadas, Voltar da tela/Android, restauração nas três etapas, cancelamento preservando conferência, divergência com produto/motivo vazios, limite de descrição, resolução/nova conferência, consulta após recebimento, erros preservando formulário e independência de cenários. Esses casos não são apresentados como ciclos red independentes.

Os testes de checklist e e-mail/senha foram deliberadamente substituídos pelas regras aprovadas. Campos serializáveis legados de Route foram preservados; cálculos novos de apresentação permanecem no estado de interface. APIs antigas sem ligação à interface permanecem para compatibilidade interna. Uma verificação binária adicional escreveu snapshots com as classes compiladas da base e os leu com as classes novas: rota aceita com conferência parcial/divergência e rota ativa foram preservadas. `Route.serialVersionUID` permanece `-8589447993292735566` em ambas as versões; evidência em `logs/legacy-binary-compatibility.log`. Nenhuma nova demonstração preenche preparo/checklist para simular a etapa retirada.

## Ambiente e matriz

AVD `Pixel_10a`, Android API 37, KVM utilizável (`emulator -accel-check`: código 0). SDK local em `/home/tambacu/Android/Sdk`, JDK 25. A matriz de revisão usa 720 × 1280 px / 320 dpi (360 × 640 dp), temas claro/escuro, fontes 100% e 200%, e limites de 359/360 dp com fonte 150%.

As duas execuções completas finais ocorreram sem filtros, após a correção do teclado:

```bash
./gradlew --no-configuration-cache :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest
# AVD em fonte 200%:
./gradlew --no-configuration-cache :app:connectedDebugAndroidTest
```

| Verificação | Resultado real |
| --- | --- |
| Build debug | Aprovado, versão 1.2 / código 3 |
| Unitários, sem filtros | 31 aprovados; zero falhas/erros/ignorados |
| Lint debug | Zero erros; 11 avisos de versões de dependências |
| Instrumentados, 360 × 640 dp / 100% | 30 aprovados; zero falhas/erros/ignorados |
| Instrumentados, 360 × 640 dp / 200% | 30 aprovados; zero falhas/erros/ignorados |
| Limite de 359 dp / 150% | 9 testes Login/Loading aprovados; ambos os temas e teclado |
| Limite de 360 dp / 150% | 9 testes Login/Loading aprovados; ambos os temas e teclado |
| Galeria | 390 PNGs íntegros; ZIP verificado |
| Git | `git diff --check` aprovado na validação inicial; integração Git posterior descrita abaixo |

Os XMLs completos incluem as dez classes instrumentadas das cinco seções, conferidas antes das verificações complementares. Temas claro/escuro são exercitados pelas suítes de revisão; manifesto e ações têm capturas com rolagem até o fim. A nova falha encontrada na primeira rodada de 200% foi conservada em `keyboard-red-200/`, corrigida e seguida por green/refactor da seção e pelas duas execuções completas acima. O repositório continua sem acesso real ao servidor.

Os limites foram executados por seção em invocações separadas. A tentativa de combinar classes em um argumento retornou somente Login; esse resultado foi contabilizado apenas como os quatro testes efetivamente listados no XML. Loading foi executado em seguida, separadamente, em cada largura. As capturas de 359 dp têm 718 × 1280 px; as de 360 dp têm 720 × 1280 px.

A galeria contém 108 capturas em 100%, 194 em 200% e 88 em 150% (44 por largura). Foram inspecionados manifesto inicial/ações, acesso e diálogo nos temas e escalas de referência, incluindo ações visíveis com teclado. Os quadros sobrepostos cobrem a rolagem completa até o último controle. Após a matriz, tamanho/densidade originais do AVD foram restaurados (1080 × 2424 px / 420 dpi), com fonte 100%.

SHA-256 do APK: `e8345c67afef1f479368155653fa2f78647afd6a05280245d7b710f770e75b01`.

## Artefatos e limites

Artefatos gerados localmente e fora do Git, no worktree de integração:

- [APK debug](../app/build/outputs/apk/debug/app-debug.apk).
- [Galeria de capturas completas](../app/build/review/access-cargo/index.html).
- [ZIP da galeria](../app/build/review/access-cargo/rotalog-1.2-acesso-recebimento.zip).
- [Relatório instrumentado 100%](../app/build/review/access-cargo/font-100/report/index.html) e [200%](../app/build/review/access-cargo/font-200/report/index.html).
- [Diff consolidado, incluindo arquivos novos](../app/build/review/access-cargo/rotalog-access-cargo-final.patch).
- [Resumo verificável](../app/build/review/access-cargo/validation-summary.json), XMLs e logs em `app/build/review/access-cargo/`.

Não foram executados testes em aparelho físico, outras APIs Android, TalkBack manual ou CI remota. A comparação direta com as telas do Stitch permaneceu indisponível pela ferramenta web. Backend, autenticação, GPS e contato real com o CD ficam fora desta demonstração. Alterações locais de `.idea` foram preservadas no worktree original e permanecem fora dos commits.


## Integração em development

Após a revisão no emulador, o usuário autorizou os commits, o merge e a atualização do remoto `origin` (`ivanmatheus-dev/rotalog-mobile`). A branch original `feature/preparacao-recebimento` passa a se chamar `development`, incorporando os merges remotos já existentes em `c77d349` (sem diferenças de arquivos em relação à base `de457d3`). A `main` continua como branch padrão do GitHub.

As frentes de Login e Rotas são registradas em commits próprios e integradas em `feature/ux-acesso-recebimento`. As correções finais de restauração, cancelamento de operações e teclado, assim como a documentação, completam a integração. Esse resultado é incorporado em `development` e enviado ao servidor. A árvore de código é conferida contra o snapshot que passou pela matriz acima; não há nova mudança de comportamento nesta operação Git.

Após o envio, as branches temporárias integradas são removidas localmente. A branch remota antiga `feature/preparacao-recebimento` é removida na mesma atualização que publica `development`. As branches antigas de interface, já incorporadas por commits equivalentes, também são removidas. Os worktrees com APK, galeria e evidências ficam preservados com HEAD destacado; a pasta original `rotalog-mobile` fica em `development`. As alterações locais de `.idea` são conferidas por SHA-256 antes e depois e não entram no histórico.
