# Login — versão 1.2

Tela inicial implementada em Kotlin/Compose, sem novas dependências. APK de revisão: `app/build/outputs/apk/debug/app-debug.apk`, versão 1.2, versionCode 3. As alterações locais preexistentes em `.idea/gradle.xml` e `.idea/misc.xml` foram preservadas. Sem commit, push ou merge.

## Referência e comportamento

Projeto Stitch: https://stitch.withgoogle.com/projects/5674232400967196939

Foram consultados os HTMLs **Acesso • Login • Claro** (`5ce3854421cf4687a7db24225b16d313`) e **Acesso • Login • Escuro** (`aba15b53113a443e8b547e79450af593`), além da imagem da referência clara. Marca com caminhão, card arredondado, campos com ícones, recuperação, botão principal e rodapé seguem a referência. Paletas e Inter reutilizam o tema existente. Campos usam rótulos flutuantes nativos Material para conservar identificação durante digitação, suporte de acessibilidade e adaptação ao tamanho da fonte.

O formulário valida e-mail e senha, limpa o erro do campo editado e permite mostrar/ocultar a senha. A ação do teclado avança do e-mail para a senha e envia o formulário na senha. O conteúdo rola com o teclado e com fonte ampliada; botões têm alvo mínimo de 48 dp, e o botão Entrar, 56 dp.

Diferenças deliberadas do protótipo: não há temporizador de sucesso nem mensagem “Operação conectada”. Com dados válidos, Entrar informa **“Autenticação indisponível nesta versão. Nenhum dado foi enviado.”**; não navega nem cria sessão autenticada. Recuperação informa que ainda não está conectada e orienta procurar a operação, sem confirmar envio de e-mail. Um aviso permanente identifica a versão demonstrativa. **Explorar demonstração** é uma ação separada que abre os fluxos fictícios anteriores sem usar credenciais.

`LoginViewModel` publica estado imutável com `StateFlow`; a tela recebe estado e callbacks. E-mail e opção de demonstração usam `SavedStateHandle` para restauração da tarefa. Senha e visibilidade permanecem somente no ViewModel, sobrevivem à rotação e não entram no estado salvo. Explorar a demonstração apaga a senha e os erros do formulário. O código não faz chamadas de rede ou registra credenciais.

## TDD e regressão

1. **Red:** antes da implementação, adicionados três testes instrumentados para entrada inicial/validação, senha/recriação e recuperação/demonstração. A execução compilou e falhou em três asserções comportamentais: ausência de “Entrar na sua conta”, do campo `login-email` e da ação “Esqueci minha senha”.
2. **Green:** implementados ViewModel, formulário e entrada inicial. Os três testes passaram no AVD. Durante a validação, a asserção inicial de visibilidade foi corrigida: Compose mantém a semântica de senha por causa do tipo de teclado mesmo quando ela está visível. O teste verifica o texto efetivamente renderizado via `GetTextLayoutResult`, incluindo os marcadores de ocultação e o conteúdo visível.
3. **Refactor:** extraída a marca para `LoginBrand`, mantendo a tela orientada por estado. Acrescentadas verificações unitárias complementares de limites de e-mail/senha, feedback, ausência de autenticação, restauração sem senha e limpeza de credenciais ao explorar a demonstração. Essas verificações complementam o ciclo conduzido pelos testes instrumentados; não são apresentadas como testes red anteriores à implementação.
4. Testes operacionais anteriores conservam suas asserções e agora entram explicitamente pela ação Explorar demonstração. Adicionado teste de revisão do login nos dois temas com teclado, indisponibilidade e erros, salvando capturas reais.

## Ambiente e evidências

JDK `/opt/android-studio/jbr`, AVD `Medium_Phone_API_37.0`, API 37, KVM confirmado por `emulator -accel-check` (código 0). Tela pequena em 720 × 1280 px a 320 dpi (360 × 640 dp). Capturas no dispositivo em `Download/RotaLogLoginReview` e cópias locais em `app/build/review/login/screenshots/`, fora do Git.

Comandos de validação:

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew \
  :app:assembleDebug :app:testDebugUnitTest :app:lintDebug \
  :app:connectedDebugAndroidTest --console=plain
```

Os resultados finais das execuções estão registrados abaixo.

- Build debug aprovado.
- 16 testes unitários aprovados (quatro complementares do login e 12 operacionais).
- 12 testes instrumentados aprovados com fonte 100%, incluindo quatro do login e oito anteriores.
- 12 testes instrumentados aprovados com fonte 200%, nos dois temas, incluindo edição com teclado e envio pela ação do teclado.
- Lint aprovado: zero erros e 11 avisos de versões mais recentes disponíveis.
- `git diff --check` aprovado.
- Capturas inspecionadas: login claro/escuro, campos e teclado, erros em fonte 200%. A fonte do AVD foi restaurada para 100% ao terminar.

A primeira execução completa com fonte 200% foi interrompida com sinal de término (saída 143), antes do relatório final. A repetição completa terminou com sucesso; somente essa repetição é contada como aprovada. Relatórios e logs preservados em `app/build/review/login/`, fora do Git; os relatórios padrão ficam em `app/build/reports/`.

SHA-256 do APK: `94b9db8c1ce955d373d28b29a76c4179f341f0cea38ae3fa424fee6cefb440e8`.

Limitação desta entrega: autenticação e recuperação reais dependem de integração com o backend. Não houve validação contra servidor, em aparelho físico ou CI remota; a revisão Android foi executada localmente no AVD com KVM.
