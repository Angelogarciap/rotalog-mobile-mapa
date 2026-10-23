# Distribuição automática do APK

Validação em 23/10/2026. Aplicativo 1.2 (`versionCode` 3), baseado no commit
`760d9ff`, com alterações locais no workflow e documentação.

## Comportamento

O workflow existente já atendia push na `main`, pull requests e execução manual.
A mudança separa o APK dos relatórios, exige sucesso nas verificações antes do
upload do APK e falha se o arquivo esperado não existir. O nome do artifact
identifica execução e commit; a retenção solicitada é de 30 dias. Relatórios
continuam sendo enviados em caso de falha. O README explica como baixar e instalar.

O APK usa a assinatura debug gerada pelo Android Gradle Plugin; não requer secrets.
Não há publicação na Play Store nem assinatura de produção. Chaves de runners
diferentes podem exigir desinstalar a versão anterior, conforme descrito no README.

## TDD e verificações

- Red: adicionados os contratos em `tools/ci/test_workflow.py` antes da alteração
  do workflow. A execução encontrou três testes: o gatilho da `main` passou;
  dois falharam com `0 != 1`, pela ausência dos artifacts separados de APK e
  relatórios. Não houve falha de ambiente ou compilação nessa etapa.
- Green: após separar os uploads e configurar identificação, retenção e erro
  para APK ausente, os três testes passaram.
- Refactor: extraída a seleção de uploads por caminho para um helper de teste;
  a suíte de contratos foi executada novamente e permaneceu verde.
- `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`:
  `BUILD SUCCESSFUL`. Build e resultados unitários foram reutilizados pelo Gradle.
- Para confirmar execução efetiva dos unitários, executado
  `./gradlew :app:cleanTestDebugUnitTest :app:testDebugUnitTest`:
  `BUILD SUCCESSFUL`, 31 testes, zero falhas, erros ou testes ignorados nos XMLs.
- `git diff --check`: aprovado.

PyYAML é usado somente na validação do contrato YAML, em ambiente virtual isolado;
não adiciona dependência ao aplicativo. O workflow também executa esses contratos.

## Limitações

A execução remota e o download real pelo GitHub Actions não foram realizados:
as mudanças ainda são locais, sem commit ou push. O teste de contrato verifica
a configuração declarada; não simula os servidores do GitHub. A primeira execução
remota deve confirmar instalação do SDK, acesso às dependências e upload.

Não executados testes instrumentados ou revisão visual em emulador: a mudança
é restrita à CI e não altera interface, fluxo do aplicativo ou suítes Android.
As alterações preexistentes em `.idea/` foram preservadas.
