# Orientações para trabalhar no RotaLog Mobile

## Projeto e contexto

- Aplicativo Android nativo do entregador, em Kotlin e Jetpack Compose.
- Consulte `README.md` para configuração, arquitetura e funcionalidades atuais.
- Consulte os documentos relevantes em `docs/` para decisões e validações anteriores.
- Mantenha versões e requisitos de ambiente no README e nos arquivos Gradle, evitando duplicá-los aqui.

## Arquitetura

- Manter fluxo unidirecional com ViewModel e StateFlow.
- Composables recebem estado e callbacks; regras operacionais e dados ficam fora deles.
- Reutilizar modelos, repositórios, temas e componentes existentes.
- Justificar novas dependências e mudanças de arquitetura.

## Interface

- Usar o projeto Stitch como referência: https://stitch.withgoogle.com/projects/5674232400967196939
- Preservar as paletas, a tipografia Inter e os temas claro e escuro.
- Usar textos em português brasileiro.
- Garantir rolagem, alvos de toque acessíveis e suporte à fonte do sistema em 200%.
- Manter dados coerentes entre todas as telas; derivar totais e progresso dos modelos.
- Identificar diferenças necessárias entre os protótipos e o comportamento implementado.

## Regras operacionais

- Aceite, preparação, recebimento e início são ações independentes, com pré-requisitos explícitos.
- Selecionar uma parada não altera seu estado operacional.
- Divergências pendentes bloqueiam o recebimento.
- Resolver uma divergência exige nova conferência do produto.
- Separar estado operacional de estado de envio.
- Identificar claramente dados e comportamentos demonstrativos.
- Nunca apresentar confirmação simulada como resposta do servidor.

## TDD obrigatório: red → green → refactor

- Toda nova funcionalidade ou comportamento executável deve ser implementado usando TDD.
- **Red:** antes de implementar o comportamento em produção, escrever um teste que expresse o resultado esperado e executá-lo, confirmando que falha pela ausência desse comportamento.
- Falhas de compilação, configuração, dependências ou ambiente não substituem a falha comportamental esperada na etapa red.
- **Green:** implementar o mínimo necessário para fazer o teste passar e executar os testes afetados.
- **Refactor:** melhorar a estrutura do código e dos testes mantendo os testes verdes; executar novamente a suíte afetada após a refatoração.
- Repetir o ciclo em incrementos pequenos. Não escrever os testes somente depois da implementação e apresentar isso como TDD.
- Testar comportamentos observáveis, regras e contratos; evitar testes que apenas reproduzam detalhes internos da implementação.
- Escolher o nível adequado: testes unitários para regras e estado; testes instrumentados para comportamentos de interface e integração Android.
- No relatório da entrega, registrar o comportamento testado, a falha observada no red e as verificações aprovadas no green/refactor.

## Correções e testes de regressão

- Antes de corrigir um bug, adicionar ou ampliar um teste que reproduza o problema e confirmar que ele falha pelo motivo esperado.
- Aplicar a correção, confirmar que o teste passa e mantê-lo na suíte como proteção permanente contra a repetição do bug.
- Cobrir os casos relacionados relevantes, incluindo limites e transições de estado quando fizerem parte da causa do problema.
- Executar os testes de regressão afetados e os fluxos relacionados para verificar que a correção não introduziu outras falhas.
- Não remover, desabilitar ou enfraquecer testes para ocultar uma regressão. Se uma regra mudar deliberadamente, atualizar o teste e documentar a mudança de comportamento.

## Validação

- Executar build, testes unitários e lint nas mudanças de código:

  ```bash
  ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
  ```

- Para alterações de interface ou fluxo, executar os testes instrumentados:

  ```bash
  ./gradlew :app:connectedDebugAndroidTest
  ```

- Validar os fluxos afetados no emulador com KVM, nos dois temas.
- Conferir telas pequenas, fonte 200% e teclado quando aplicável.
- Informar as verificações não executadas e seus impedimentos. Não declarar um teste aprovado sem executá-lo.

## Git e arquivos locais

- Conferir o estado do Git antes de alterar arquivos.
- Preservar as mudanças existentes do usuário.
- Manter configurações locais, credenciais e artefatos gerados fora dos commits.
- Conferir o diff e todo o conteúdo staged antes de cada commit, incluindo arquivos adicionados por ferramentas externas.
- Fazer push ou merge quando solicitado.

## Documentação e comunicação

- Atualizar `README.md` quando mudar a execução ou o comportamento do aplicativo.
- Registrar validações relevantes em `docs/`.
- Ao concluir, informar as mudanças, os testes executados, as limitações e a versão para revisão.
