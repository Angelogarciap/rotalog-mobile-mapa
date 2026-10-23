# Perfil — refinamento visual da versão 1.2

Implementação restrita ao Perfil, sem novas dependências, mantendo assinatura pública, callbacks, modelos, paletas e Inter. Versão para revisão: 1.2, versionCode 3. APK em `app/build/outputs/apk/debug/app-debug.apk`. As alterações preexistentes em `.idea` foram preservadas. Validação registrada antes do commit; sem push ou merge.

## Referência e diferenças deliberadas

Consultados os HTMLs do projeto [Stitch](https://stitch.withgoogle.com/projects/5674232400967196939): **Final • Perfil e Preferências** (`9bef737ca57641aab630f7cd313b26e0`) e **Escuro • Perfil e preferências** (`272abbe37b854d4c927e6c9248c67dea`). Cópias locais em `app/build/review/profile/stitch-*.html`.

- Ordem: identidade, veículo, aparência, ambiente demonstrativo e rodapé. Nome permite quebra; avatar azul com iniciais CE, função e MOT-7832, selo de identidade demonstrativa. Veículo mantém Renault Master e placa PHB-9A23.
- Reutilização de `SectionCard`, `StatusBadge`, ícone de caminhão e tokens atuais. Os cards do Perfil acrescentam sombra de 2 dp e contorno de 1 dp com `outline` a 40%, preservando superfície branca no claro e superfície existente no escuro, cantos de 20 dp, padding de 16 dp e intervalo entre cards de 16 dp. A paleta global permanece intacta.
- Tema escolhido tem fundo tonal, borda azul de 2 dp e marca de seleção. As três opções usam grupo de seleção e papel acessível de rádio, sem alvos duplicados. Abaixo de 360 dp ou a partir de fonte 150%, ficam empilhadas. Conteúdo sem altura fixa, controles com mínimo de 48 dp.
- Ambiente demonstrativo começa recolhido; aviso de dados fictícios e resumos do cenário/envio continuam visíveis. Ao abrir, conserva três cenários, reinício e seis estados de envio, com os mesmos callbacks, rótulos e regras. Recolher altera apenas apresentação.
- Expansão usa `rememberSaveable`; rolagem usa `rememberScrollState`. A restauração existente da navegação conserva ambos na troca de aba e recriação da Activity. Não se trata de persistência durável da operação.
- Métricas, permissões concedidas, conexão, sincronização real e encerramento de sessão do protótipo não foram incorporados. Nenhuma confirmação simulada é apresentada como resposta de servidor. Teclado não se aplica ao Perfil, que não contém campos de entrada.

## TDD e regressão

1. **Red:** `ProfileFlowTest` foi escrito e executado antes de editar produção. Os três testes compilaram e falharam por comportamento: `demoStartsCollapsedAndToggleOnlyChangesPresentation` encontrou o reinício quando deveria estar ausente; `expansionAndScrollSurviveRecreationAndTabReturn` encontrou os controles após tentar recolher; `themesExposeSingleAccessibleSelectionAndCheckmark` não encontrou a marca “Tema selecionado”. Log e resultados preservados em `app/build/review/profile/red.log` e `red-results/`.
2. **Green:** após implementar os cards, opções visuais e expansão salvável, os três testes instrumentados passaram (`green.log`). Verificam também que abrir/recolher não muda a rota, manutenção da seleção após recriação e manutenção do último controle visível após recriação/troca de aba.
3. **Refactor:** extraída `ThemeOptions`, mantendo `ProfileCard`, `ThemeOption` e os controles operacionais existentes. Reforçada a asserção de uma única marca e opções restantes não selecionadas. Os testes operacionais antigos agora abrem a seção quando necessário; suas asserções foram mantidas.
4. **Segundo ciclo red → green:** a inspeção das capturas revelou alturas desiguais nas opções lado a lado. Antes da correção, um teste instrumentado confirmou a falha esperada: 123,5 dp para aparelho contra 75,5 dp para Claro (`red-theme-height-behavior.log`, `red-height-results/`). O ajuste usa altura intrínseca na linha e preenchimento de altura nas opções. O teste final `themeOptionsHaveAccessibleAdaptiveLayout` também verifica alvo mínimo de 48 dp e empilhamento sem sobreposição em largura pequena/fonte ampliada. Uma tentativa anterior desse teste não compilou por usar uma propriedade de altura ausente em `DpRect`; foi corrigida antes do red comportamental.
5. `ProfileReviewTest` acrescenta revisão complementar: captura viewports sobrepostos de todo o Perfil, nos dois temas, recolhido e expandido, até o rodapé, verificando acesso a Conflito. Não é apresentado como teste red anterior à implementação.

A primeira tentativa da validação completa encontrou erro de compilação no teste de capturas por uso de uma extensão de rolagem indisponível. O teste passou a executar a ação semântica `ScrollBy`. Esse erro de ferramenta não integra a etapa red.

## Ambiente e evidências

JDK `/opt/android-studio/jbr`; SDK local; AVD `Pixel_10a`, API 37. `emulator -accel-check` confirmou KVM utilizável (código 0). Tela pequena configurada em 720 × 1280 px / 320 dpi (360 × 640 dp). Capturas reais em `Download/RotaLogProfileReview` no dispositivo e cópias em `app/build/review/profile/`, fora do Git.

A revisão inicial passou com 16 testes instrumentados em fonte 100%. Após revisar as capturas, a medição de largura passou de `Configuration.screenWidthDp` para `LocalWindowInfo.current.containerSize`, convertida pela densidade. Isso atende ao tamanho da janela e evita o novo aviso de lint sem suprimir verificações.

## Verificações finais

- `JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest`: aprovado após refatoração e ajuste das alturas (`final-100.log`).
- Build debug aprovado; 16 testes unitários aprovados; lint com zero erros e os mesmos 11 avisos anteriores de atualização de versões.
- 17 testes instrumentados aprovados em 360 × 640 dp com fonte 100%, sem falhas ou testes ignorados (`results-100/`).
- `:app:connectedDebugAndroidTest`: 17 testes aprovados com fonte 200%, sem falhas ou testes ignorados, incluindo os dois temas e fluxos relacionados (`final-200.log`, `results-200/`).
- Quatro testes do Perfil aprovados em 359 dp / fonte 100% (`width-359.log`, `results-359/`) e quatro em 360 dp / fonte 150% (`font-150.log`, `results-150/`), incluindo empilhamento, alvos de toque e restauração.
- `git diff --check`: aprovado.
- Inspeção visual das capturas reais: contornos/sombras nos dois temas, nome em várias linhas, placa, tema selecionado, opções empilhadas em fonte 200%, cenários e estados de envio, acesso a Conflito e rodapé. As sequências usam sobreposição para cobrir todo o conteúdo; partes fora de cada viewport são acessadas por rolagem.

Artefatos locais de revisão (fora do Git):

- [Galeria das 52 capturas finais](../app/build/review/profile/index.html), ordenadas por tema, expansão e fonte. A galeria permite percorrer as sequências e abrir PNGs no tamanho original.
- [Capturas em ZIP](../app/build/review/profile/profile-captures.zip), incluindo a galeria para abrir localmente.
- [APK debug 1.2 / versionCode 3](../app/build/outputs/apk/debug/app-debug.apk).
- Logs, XMLs de testes e referências em `app/build/review/profile/`; relatórios padrão em `app/build/reports/`.

SHA-256 do APK: `417073bc4db9f36eeebdc8e288b89c0108572d39e76bea1056946ffa446abbe1`.

Limitações: validação local em emulador Android API 37 com KVM. Não executada em aparelho físico, outras versões de Android ou CI remota. Nenhuma integração com servidor foi adicionada ou validada. Teclado não se aplica à tela de Perfil; os testes de login/teclado existentes passaram nas suítes completas.

Ao terminar, fonte do AVD restaurada para 100%, tamanho e densidade restaurados aos valores originais (1080 × 2424 px / 420 dpi).
