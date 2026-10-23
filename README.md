# RotaLog Entregador

Aplicativo Android nativo para o motorista do marketplace B2B RotaLog. Esta primeira entrega contém uma demonstração operacional com Kotlin, Jetpack Compose e Material 3.

## O que está implementado

- **Rotas:** rota atribuída, confirmação explícita de aceite e indicação independente de preparação e recebimento pendentes.
- **Rota ativa demonstrativa:** oito paradas na ordem da operação, mapa ilustrativo nativo, progresso, lista e painel arrastável com detalhes, contato e produtos.
- **Ações externas:** escolha de Google Maps, Waze ou copiar endereço; chamada pelo discador. Aplicativos ausentes recebem mensagem e alternativa de copiar o endereço.
- **Histórico:** uma rota anterior fictícia concluída, independente da rota atual.
- **Perfil:** identidade demonstrativa, tema do aparelho/claro/escuro, três cenários e seis estados de envio simulados.

Ao abrir, o cenário padrão é **Rota atribuída**. Aceitar a rota apenas confirma o aceite: não recebe mercadorias, prepara o veículo, inicia rota ou coleta localização. No Perfil, **Rota ativa demonstrativa** carrega um exemplo independente, com duas de oito paradas concluídas. Selecionar uma parada nunca altera seu estado operacional.

A carga total é calculada a partir dos itens: **70 volumes previstos para coleta e 70 para entrega**, sem somar coleta e entrega como cargas diferentes. Na rota ativa, 70 foram coletados e oito entregues. O Supermercado Amazonas tem 14 volumes: oito caixas de óleo, quatro fardos de arroz e duas caixas de café.

O mapa exibe **“Mapa demonstrativo • sem GPS”**. Os caminhos são vetoriais ilustrativos, sem cálculo de trajetos pelas ruas. Nenhuma permissão de GPS, câmera ou chamada direta é solicitada.

## Executar

Requisitos: JDK 25, Android SDK `platforms;android-37.0`, Build Tools 36.0.0 e dispositivo/emulador API 29+. Configure `sdk.dir` no `local.properties` (fora do Git) ou `ANDROID_HOME`.

```bash
export JAVA_HOME=/opt/android-studio/jbr # adapte ao JDK 25 local
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

No Android Studio, abra a raiz, aguarde o Gradle Sync e execute `app`. No Windows, use `gradlew.bat`.

APK: [`app/build/outputs/apk/debug/app-debug.apk`](app/build/outputs/apk/debug/app-debug.apk).

## Verificar

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
# Com dispositivo/emulador conectado:
./gradlew :app:connectedDebugAndroidTest
```

Os testes cobrem aceite repetido, pré-requisitos, isolamento de cenários, totais, próxima parada, seleção, recriação, temas, envio simulado e contratos de intents com aplicações ausentes. A CI executa build, testes unitários e lint na raiz; publica APK e relatórios como artifacts. Os testes instrumentados são executados localmente no AVD com KVM.

Consulte o registro da entrega em [docs/VALIDATION.md](docs/VALIDATION.md).

## Arquitetura

Um módulo `:app`, composição manual de dependências, fluxo unidirecional e estado imutável:

```text
com.rotalog.delivery/
  AppContainer.kt        # dependências com escopo da aplicação
  MainActivity.kt        # bootstrap e Navigation Compose
  model/                 # rotas, paradas, itens e estados independentes
  data/                  # contratos, fixtures e DataStore
  routes/                # ViewModel + StateFlow, telas e mapa Canvas
  history/               # histórico demonstrativo
  profile/               # preferências e controles da demonstração
  shared/                # componentes, vetores e intents Android
  ui/theme/              # paletas fixas, Inter, formas e espaçamento
```

O `RoutesViewModel` pertence ao grafo principal e é compartilhado entre Rotas, lista de paradas e controles do Perfil. A navegação transporta apenas `routeId`. `SavedStateHandle` restaura cenário, seleção, expansão, aceite e estado de envio em recriação; a navegação e a rolagem usam a restauração do Compose. O repositório demonstrativo permanece na memória da aplicação. **Somente a preferência de tema tem persistência durável**, em DataStore; encerrar o processo retorna a operação à demonstração padrão. Não existe fila offline real.

Os seis estados de envio (salvo no aparelho, aguardando, enviando, confirmado, falha e conflito) são escolhidos manualmente no Perfil e identificados como simulados. Não há confirmação automática por conectividade ou temporizador. “Confirmado” não muda a situação operacional da rota.

Versões de referência preservadas: AGP 9.4.1, Gradle 9.6.0, Kotlin 2.2.10, Compose BOM 2026.02.01, `compileSdk/targetSdk 37`, `minSdk 29`. Dependências adicionais: Navigation Compose 2.9.5, Lifecycle Compose 2.9.4 e DataStore Preferences 1.1.7. AndroidX Test JUnit 1.3.0 e Espresso 3.7.0 substituem os testes antigos incompatíveis com o Android 37 (`InputManager.getInstance`). Nenhuma atualização da stack principal foi necessária. Fonte Inter e licença: [docs/licenses](docs/licenses/README.md).

## Limites e próxima entrega

Não há backend, autenticação, recebimento completo, checklist operacional, evidências, ocorrências, devoluções, encerramento, mapas reais ou rastreamento. O histórico não fornece comprovantes reais. Telefone e identidade são fictícios.

A próxima etapa é **preparação e recebimento da carga**, mantendo seus pré-requisitos independentes do aceite. Depois, integrar o contrato canônico OpenAPI do backend. Não são necessárias credenciais externas nesta demonstração.

Para substituir o mapa demonstrativo por Google Maps SDK/Maps Compose, serão necessários projeto Google Cloud, faturamento, SDK habilitado e chave restrita ao aplicativo fora do Git. O SDK desenhará ruas; geometria de percurso deverá vir da operação ou de serviço de rotas.

## Referências

- [Design no Stitch](https://stitch.withgoogle.com/projects/5674232400967196939)
- [Navigation Compose](https://developer.android.com/develop/ui/compose/navigation)
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- [Canvas no Compose](https://developer.android.com/develop/ui/compose/graphics/draw/overview)
- [AndroidX Test: versões e notas de compatibilidade](https://developer.android.com/jetpack/androidx/releases/test)
- [Maps Compose](https://developers.google.com/maps/documentation/android-sdk/maps-compose)
- [Backend](https://github.com/Uninorte-Extensao/rotalog-backend) · [Portal](https://github.com/Uninorte-Extensao/rotalog-frontend)
