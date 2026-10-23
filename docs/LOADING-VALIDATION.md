# Preparação e recebimento — versão 1.1

Implementação na branch `feature/preparacao-recebimento`, a partir de `feature/primeira-entrega-compose` (`ca1cda5`). Os ajustes locais do Android Studio foram preservados. Não houve push nem merge na `main`.

## Referências do Stitch

Projeto: https://stitch.withgoogle.com/projects/5674232400967196939

Foram lidos HTML e imagem da tela **Final • Recebimento da Carga no CD**, além do HTML de **Escuro • Recebimento da carga** e **Final • Rota Atribuída e Checklist**. A implementação segue os cards, cabeçalho com origem/doca, progresso em quatro etapas, checklist, conferência, formulário de divergência e confirmação de recebimento.

Os dados continuam coerentes com a primeira entrega: CD Central Manaus, doca de expedição 2 e 70 volumes da rota inteira. Os 14 volumes do Supermercado Amazonas são parte dessa carga. O manifesto é derivado dos itens das entregas: oito produtos, sem duplicar o total entre coleta e entrega.

Rótulos das referências que indicavam GPS, conexão com o CD, assinatura ou notificação real foram substituídos por informações sobre a demonstração. Não há implementação simulando confirmação do servidor.

## Fluxo disponível

1. **Aceitar rota** e confirmar o aceite. Preparo e recebimento permanecem pendentes.
2. **Preparar saída**: marcar combustível, pneus, bateria e área de carga; confirmar o checklist.
3. **Avançar para conferência**: conferir cada produto e seus volumes físicos.
4. Se houver falta, avaria, lote/produto divergente ou vazamento, registrar a divergência e observação opcional (até 500 caracteres).
5. Após resolver com a equipe do CD, confirmar a resolução no app e conferir novamente o produto.
6. **Confirmar recebimento da carga**, com confirmação explícita. A coleta é concluída, mas a rota continua aguardando início.
7. Voltar à rota, escolher **Iniciar rota** e confirmar **Iniciar agora**. O mapa abre na Drogaria Rio Negro, com uma parada de coleta concluída e zero entregas concluídas.

O exemplo ativo selecionável no Perfil permanece independente e começa com duas paradas concluídas, na próxima parada Supermercado Amazonas.

## Regras e estado

- Repositório valida rota atual, aceite, checklist, conferência completa, divergências e partida. Botões desabilitados refletem essas regras, mas não são a única proteção.
- Marcar os quatro itens não confirma o preparo automaticamente.
- Conferir os oito produtos não confirma o recebimento automaticamente.
- Registrar uma divergência remove a marca de conferência do produto.
- Registrar a resolução não confere o produto automaticamente.
- Receber não inicia a rota; iniciar não conclui entregas.
- Após receber, a conferência fica disponível para consulta. Novas divergências e alterações de conferência ficam bloqueadas.
- Desmarcar um item do checklist revoga o preparo sem apagar o recebimento já confirmado. A partida fica bloqueada até reconfirmar o checklist.
- Confirmações repetidas não duplicam coleta ou partida.
- Tocar no cenário já selecionado preserva o progresso. Reiniciar exige o controle explícito do Perfil; trocar o cenário carrega sua fixture independente.
- O estado da rota é salvo em `SavedStateHandle` como snapshot serializável, separado dos comandos operacionais. Checklist, conferência, divergências, cenário e estado operacional são restaurados na recriação da tarefa. O formulário usa `rememberSaveable` para restaurar produto, motivo e observação.
- Somente o tema tem persistência durável própria em DataStore. Os dados operacionais continuam demonstrativos; não existe banco local ou fila de envio.

## Executar e revisar

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:assembleDebug :app:installDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`, versão 1.1, versionCode 2.

SHA-256: `6219d2d86f579ec90601fb9313fc2dfa5c5f06b752fd63a65d3f64f45807502f`.

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew \
  :app:assembleDebug :app:testDebugUnitTest :app:lintDebug \
  :app:connectedDebugAndroidTest --console=plain
```

Ambiente: JDK 25, SDK 37, AVD `Medium_Phone_API_37.0` com KVM confirmado por `emulator -accel-check` (código 0). Tela ajustada para 720 × 1280 px a 320 dpi, equivalente a 360 × 640 dp.

- Build debug aprovado.
- 12 testes unitários aprovados.
- 8 testes instrumentados aprovados com fonte 100%.
- 8 testes instrumentados aprovados com fonte 200%, incluindo observações com teclado e restauração do formulário.
- Lint: 0 erros; 11 avisos de versões mais recentes disponíveis.
- `git diff --check` aprovado.

Os testes exercitam o fluxo completo pela interface, cancelamento das confirmações, bloqueio por divergência, resolução e nova conferência, primeira entrega pendente, checklist parcial restaurado, observação digitada com teclado e formulário restaurado após recriar Activity. Também preservam os testes de mapa, painel, temas, cenários e ações externas da primeira entrega.

Evidências locais, fora do Git:

- `app/build/reports/tests/testDebugUnitTest/index.html`;
- `app/build/reports/lint-results-debug.html`;
- `app/build/reports/androidTests/connected/debug/index.html`;
- `app/build/review/loading/font-100/` e `font-200/`: resultados instrumentados por escala;
- `app/build/review/loading/screenshots/`: checklist, conferência, divergência e recebimento nos dois temas e escalas.

O teste salva capturas em `Download/RotaLogLoadingReview` no AVD via MediaStore, sem permissão de armazenamento. A fonte é restaurada para 100% ao terminar a revisão. A CI existente compila e executa os testes unitários e lint; não foi disparada remotamente.

## Próxima etapa

**Execução da entrega na parada**: início da tentativa, dados do recebedor e resultado da entrega. Fotos, assinatura, ocorrências de entrega, backend, sincronização real e GPS continuam fora desta versão.
