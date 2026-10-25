# Redesign visual do login — 23/10/2026

Revisão local da versão 1.2 (versionCode 3). Alterações restritas à tela de login, testes e documentação. ViewModel, autenticação, navegação, repositórios e contratos preservados. Sem novas dependências. Mudanças preexistentes nos três arquivos `.idea/` preservadas.

## Direção visual e pesquisa

A composição atual usa marca ampliada, Inter, título e descrição com hierarquia clara, rótulo fixo acima do campo, superfície suave e botão azul. Demonstração tem menor peso tipográfico e informações técnicas ficam no rodapé. Sem cards empilhados, sombras ou ilustrações adicionadas. Temas e tokens existentes foram reutilizados; não houve alteração global da paleta.

Após revisão do usuário, o trajeto decorativo foi retirado: primeiro, a baixa opacidade comprometia sua visibilidade; aumentar a saturação tornou a composição pesada. A solução final prioriza tipografia, alinhamento e campo claramente identificável. A retirada é uma decisão visual deliberada, sem remover informação ou ação. O teste experimental de contraste desse desenho foi substituído pelo contrato visual do rótulo fixo, pois o desenho deixou de existir; testes de contraste textual e foco foram mantidos.

Referências consultadas:

- [Android: Authentication & Onboarding](https://developer.android.com/design/ui/mobile/guides/patterns/onboarding): entrada rápida, linguagem clara, proximidade entre elementos relacionados e largura limitada para formulários.
- [Material: Text fields](https://m2.material.io/design/components/text-fields.html): identificação do campo, rótulos legíveis, estados e contraste de contornos.
- [WCAG: contraste de texto](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html): contraste de pelo menos 4,5:1 em texto comum, incluindo placeholders.
- [WCAG: contraste não textual](https://www.w3.org/WAI/WCAG22/understanding/non-text-contrast.html): distingue gráficos informativos e decoração; contraste não exige saturar toda a decoração.
- [NN/g: login mobile](https://www.nngroup.com/articles/checklist-registration-login/): minimizar esforço e quantidade de informação solicitada. Nenhum novo mecanismo de autenticação foi introduzido.
- Pesquisa visual de telas de login de Revolut/Monzo e [Refero](https://refero.design/apps/search?app_id%5Bid%5D%5B%5D=74&design_patterns%5Bid%5D%5B%5D=120), usada como inspiração de hierarquia, sem copiar assets.
- Referência original do projeto: Stitch `5674232400967196939`, HTML da tela **Acesso • Login • Claro** (`5ce3854421cf4687a7db24225b16d313`). Diferenças deliberadas: sem card, sem fluxo de senha e sem autenticação simulada.

## Acessibilidade

Rótulo visível fica acima do campo em todos os estados; o campo recebe o mesmo nome acessível. A cópia externa do rótulo é ignorada pelo serviço de acessibilidade para evitar leitura duplicada. Foco por teclado: identificador → Entrar → demonstração; Shift+Tab faz o caminho inverso, sem elementos decorativos no percurso. O título conserva semântica de cabeçalho e a indisponibilidade conserva região de anúncio acessível.

Medições pelos pares de cores definidos (sem arredondar valores para decidir aprovação):

| Par | Contraste aproximado |
| --- | --- |
| Texto secundário claro / fundo | 7,24:1 |
| Texto secundário escuro / fundo | 7,30:1 |
| Placeholder claro / campo | 6,92:1 |
| Placeholder escuro / campo | 5,71:1 |
| Ação demonstrativa clara / fundo | 4,94:1 |
| Ação demonstrativa escura / fundo | 7,36:1 |
| Texto branco / botão principal | 5,17:1 |

Os testes leem a cor efetiva dos textos no Compose e a comparam com o fundo correspondente. Cobrem marca, título, descrição, informações secundárias, demonstração, rótulo, identificador digitado, placeholder, erro, mensagem de indisponibilidade e botão principal nos dois temas. Tamanhos permanecem em sp e o conteúdo rola com fonte 200% e teclado. Alvos mínimos dos botões: 56 e 48 dp.

## TDD e regressão

1. Composição inicial: teste acrescentado antes da implementação falhou porque cabeçalho e apresentação estavam separados por mais de 96 dp. Depois da composição integrada, passou com os testes de login existentes.
2. Contraste do desenho: teste renderizado reproduziu contraste de apenas 1,69:1. Uso de azul sem transparência passou, mas foi rejeitado visualmente pelo usuário; esse desenho foi posteriormente removido conforme descrito acima.
3. Rótulo fixo: antes da mudança em produção, `identifierLabelStaysAboveFieldWhileEditingInBothThemes` compilou e falhou com **“Rótulo deve permanecer acima do campo, sem sobreposição”**. Após a alteração, passou antes, durante e depois da edição, nos dois temas. XML red em `app/build/review/login-refined/red/`.
4. O teste de foco usa eventos reais de teclado via Espresso; a primeira tentativa com injeção interna do Compose não ativava o modo de entrada por teclado e foi corrigida no teste. A ordem nativa existente passou, sem alterações de navegação.
5. Green: cinco testes unitários de Login e oito instrumentados aprovados em 360 × 640 dp / 100%. Refino de código limitado a organizar o import de FontWeight; nova execução dos instrumentados em 200% e regressão final registradas abaixo.

## Ambiente e evidências

AVD `Pixel_10a`, API 37, KVM confirmado (código 0 em `emulator -accel-check`), JDK 25. Revisão em 720 × 1280 px / 320 dpi (360 × 640 dp), temas claro/escuro e fontes 100%/200%, incluindo teclado, erros e última ação.

Capturas atuais e relatórios em `app/build/review/login-refined/`, fora do Git. [Claro](../app/build/review/login-refined/light.png), [escuro](../app/build/review/login-refined/dark.png) e [galeria](../app/build/review/login-refined/index.html). As evidências em `login-redesign/` e `login-contrast/` são de iterações anteriores.

## Validação final

Comandos sem filtros executados:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest --no-configuration-cache
```

- Build debug aprovado.
- 31 testes unitários aprovados.
- 34 instrumentados da suíte completa aprovados em fonte 100%, abrangendo todas as cinco seções. Zero falhas, erros ou ignorados.
- Oito instrumentados de Login aprovados adicionalmente em fonte 200%, incluindo contraste e foco nos dois temas.
- Lint aprovado: zero erros; 12 avisos (11 de versões de ferramentas/dependências e um uso preexistente de `Configuration.screenHeightDp` fora do login).
- `git diff --check` aprovado.
- Tamanho/densidade originais e fonte 100% restaurados no AVD.

APK: [app-debug.apk](../app/build/outputs/apk/debug/app-debug.apk), versão 1.2 / versionCode 3. SHA-256: `b616cc3d4ee90729e1d2a03b228aa6404124a57250953949a76b36a8cf02eccf`.

Limitações: revisão em emulador, sem aparelho físico, CI remota ou sessão manual com TalkBack. O foco de teclado foi exercitado por eventos reais do Android e a semântica de nome do campo foi definida no Compose. A suíte completa foi executada em fonte 100%; a execução extra em 200% foi limitada ao Login, única seção visual alterada. Autenticação continua indisponível nesta versão.
