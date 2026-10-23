# Registro de validação — primeira entrega nativa

Data: 22/10/2026 (America/Manaus).

## Versão para revisão

- Branch integrada: `feature/primeira-entrega-compose`.
- Projeto: `/home/tambacu/Projects/rotalog-mobile`.
- APK debug: `app/build/outputs/apk/debug/app-debug.apk` (~32 MB).
- SHA-256 do APK: `ea1ec61598f538696f9cfe63f40da15849e08cf3de2151d53d40d5b87bacb0a2`.
- Não houve push nem merge na `main`.
- As alterações iniciais de AGP 9.4.1 e Wrapper 9.6.0 foram preservadas e incorporadas à entrega. As configurações locais `.idea/gradle.xml`, `.idea/misc.xml` e `.idea/.name` continuam locais, sem inclusão no commit.

## Ambiente e comandos

JDK 25.0.3 do Android Studio, SDK `android-37.0`, Build Tools 36.0.0, AVD `Medium_Phone_API_37.0`, Android API 37 (Android 17), x86_64.

Fora do sandbox, `emulator -accel-check` retornou código 0 e `KVM (version 12) is installed and usable`. O AVD foi iniciado com `-accel on`, `-no-window`, `-no-snapshot` e renderização SwiftShader. Assim, a virtualização da CPU usou KVM; a renderização gráfica usou software.

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew \
  :app:assembleDebug :app:testDebugUnitTest :app:lintDebug \
  :app:connectedDebugAndroidTest --console=plain
```

| Verificação | Resultado |
|---|---|
| APK debug | Compilado e instalado no AVD |
| Testes unitários | 6 aprovados, sem falhas |
| Testes instrumentados, fonte 100% | 6 aprovados, sem falhas |
| Testes instrumentados, fonte 200% | 6 aprovados, sem falhas |
| Lint debug | 0 erros; 11 avisos de versões mais recentes disponíveis |
| `git diff --check` | Aprovado |
| Revisão visual | Capturas dos temas claro e escuro em 360 × 640 dp, com fonte 100% e 200% |
| CI Android | Workflow adaptado; comandos verificados localmente. Execução remota exige push/PR e não foi realizada |

Os testes de exemplo foram substituídos por testes das regras e dos fluxos desta entrega. A infraestrutura antiga de Espresso 3.5.1 falhava no Android 37 com `NoSuchMethodException: InputManager.getInstance`. Espresso 3.7.0 e AndroidX Test JUnit 1.3.0 resolveram a incompatibilidade. As versões principais solicitadas permaneceram iguais.

## Comportamentos conferidos

- Confirmação e cancelamento do aceite. Aceite repetido é idempotente e não recebe carga, prepara saída ou inicia rota.
- Cenários atribuído, vazio e ativo independentes; reiniciar restaura a fixture original.
- Sequência de oito paradas, próxima parada, progresso de 2/8 e totais derivados. Coletados 70/70 e entregues 8/70 na fixture ativa.
- Supermercado Amazonas: endereço e Roberto (gerente), oito caixas de óleo, quatro fardos de arroz e duas caixas de café, totalizando 14 volumes.
- Seleção por toque no mapa e na lista; expansão, retorno à próxima parada e estado operacional preservado.
- Painel nativo arrastável com botão fixo; expansão/recolhimento, rolagem e restauração após recriar Activity.
- Marcador selecionado totalmente acima do painel expandido, sem sobreposição pela legenda, inclusive em fonte 200%.
- Abas, retorno da lista e restauração de navegação/rolagem pelo Compose.
- Tema do aparelho, claro e escuro. Verificação adicional manual: selecionar Escuro, encerrar com `am force-stop`, reabrir, conferir tela escura e opção Escuro ainda marcada no Perfil.
- Estados de envio escolhidos manualmente; avanço do relógio não confirma envio e não muda estado operacional.
- Contratos de Google Maps/Waze com endereço codificado, discador `ACTION_DIAL` e recuperação de `ActivityNotFoundException`; copiar endereço validado pela interface. Aplicativos indisponíveis mantêm as alternativas de navegação/cópia. O teste usa lançador injetável para tornar a indisponibilidade reproduzível.
- Barras de status e navegação acompanham o tema; fonte do sistema respeitada e detalhes acessíveis por rolagem.

Não houve validação de trajetos reais de Maps/Waze, chamadas telefônicas nem comunicação com servidor: esses recursos não fazem parte da demonstração.

## Evidências locais

Gerados, fora do versionamento:

- `app/build/reports/tests/testDebugUnitTest/index.html`;
- `app/build/reports/lint-results-debug.html`;
- `app/build/reports/androidTests/connected/debug/index.html`;
- `app/build/review/font-100/test-results/` e `font-200/test-results/`;
- `app/build/review/screenshots/` — capturas reais do AVD de rota atribuída, ativa, detalhes, produtos, Histórico e Perfil, nos dois temas e nas duas escalas;
- `app/build/review/screenshots/dark-after-restart.png` — evidência do tema após reinício do processo.

O teste `ReviewScreensTest` salva as imagens em `Download/RotaLogReview` no AVD via MediaStore, sem permissão de armazenamento. Os testes Gradle removem automaticamente seus APKs ao terminar; as capturas em Downloads permanecem. Para repetir:

```bash
adb shell wm size 720x1280
adb shell wm density 320
adb shell settings put system font_scale 2.0
./gradlew :app:connectedDebugAndroidTest
adb pull /sdcard/Download/RotaLogReview ./app/build/review/screenshots
adb shell settings put system font_scale 1.0
```

## Integração das frentes

| Branch | Worktree mantido | Commit de origem integrado |
|---|---|---|
| `feature/visual-system` | `/tmp/rotalog-visual` | `d3ab697` |
| `feature/routes-ui` | `/tmp/rotalog-routes` | `833385c` |
| `feature/history-profile` | `/tmp/rotalog-profile` | `a3a6a2b` |

Os commits das três frentes foram integrados por cherry-pick. Modelos, repositórios, DataStore, ViewModels, bootstrap, navegação, intents, testes, CI e ajustes finais de integração estão na branch de entrega. Nenhum worktree foi removido.

## Limites e continuidade

Mapa e envios são demonstrativos, sem GPS ou backend. O tema é a única preferência gravada de forma durável; a operação usa fixtures em memória e estado salvo pelo Android para recriação da tarefa. Não há fila offline ou confirmação real de servidor.

Próxima etapa recomendada: **preparação e recebimento da carga**, seguida da integração com os contratos OpenAPI. A demonstração não exige chave de mapas ou configuração externa adicional.
