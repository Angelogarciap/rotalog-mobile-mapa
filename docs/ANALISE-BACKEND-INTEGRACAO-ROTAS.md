# Análise do backend para integração das rotas no RotaLog Mobile

**Data:** 23/10/2026, com conferência do ambiente às 18h04, horário de Manaus (UTC−04:00).
**Finalidade:** repasse técnico ao responsável pelo backend e a uma IA que auxiliará na definição e implementação futura dos contratos.
**Situação deste documento:** diagnóstico e proposta para discussão. Não representa uma implementação nem um contrato aprovado.

## 1. Orientação para quem receber este documento

Leia este documento como uma comparação entre o backend disponível e o comportamento esperado pelo aplicativo Android. As seções sobre o estado atual descrevem constatações no código e no ambiente. As sugestões de endpoints, estruturas e sequência de trabalho são recomendações e precisam ser avaliadas pelo responsável.

O pedido que originou esta análise foi **somente analisar**, sem implementar funcionalidades ou modificar dados. A produção deste documento não autoriza automaticamente alterações no backend, no aplicativo, no banco ou na infraestrutura. Antes de uma implementação futura, o responsável deve confirmar o escopo e resolver as decisões de produto indicadas aqui.

Ao usar uma IA para dar continuidade:

1. Confira novamente o código e o OpenAPI, pois este diagnóstico corresponde às versões abaixo.
2. Preserve o fluxo operacional definido no aplicativo, salvo mudança de regra expressamente aprovada.
3. Separe funcionalidades existentes, funcionalidades ausentes e decisões ainda abertas.
4. Não interprete nomes de campos, estados previstos no banco ou exemplos demonstrativos como funcionalidades completas.
5. Não substitua confirmações do servidor por sucessos simulados ou mudanças exclusivamente locais.
6. Não implemente literalmente todos os endpoints sugeridos sem verificar se um contrato menor atende às mesmas regras.

## 2. Projetos e fontes analisados

Os repositórios estão lado a lado:

```text
/home/tambacu/Projects/
  rotalog-mobile/
  rotalog-backend/
```

| Referência | Versão consultada |
|---|---|
| HEAD do mobile | `36c3415dbb727d6a122591090dde01d3a46689d6` |
| Versão declarada do Android | `1.2`, versionCode `3` |
| HEAD do backend | `cf58f7dba4c699035a1a8980b5272fbe030359be` |
| Versão declarada pela API | `0.2.0` |
| Swagger local | `http://127.0.0.1:8000/docs` |
| OpenAPI local | `http://127.0.0.1:8000/openapi.json` |

O mobile já tinha alterações locais em arquivos de IDE, login, testes e documentação. Elas foram preservadas. O backend estava sem alterações locais segundo o Git. Portanto, o diagnóstico do aplicativo considera também o conteúdo presente no diretório de trabalho, não apenas o commit informado.

Foram consultados README, regras do projeto Android, modelos, repositório demonstrativo, contratos, telas, serviços, autorização, testes existentes e documentação do backend. Os hashes dos arquivos de router, serviço e schemas de logística e dos serviços de identidade e pedidos foram comparados com os arquivos do container da API: eram iguais.

## 3. Conclusão principal

**O backend possui autenticação, atribuição simples de rota e rastreamento, mas ainda não fornece os dados e as regras necessários ao fluxo operacional completo do aplicativo.**

A principal diferença não é apenas a falta de endpoints. Hoje, o backend representa uma rota associada a um pedido de fornecedor. O aplicativo representa um itinerário com coleta, várias entregas e retorno à base, incluindo aceite, conferência, divergências e recebimento da carga.

Conectar somente os endpoints atuais permitiria trabalhar com identidade, lista resumida de rotas, início, encerramento de rastreamento e localização. Isso não permitiria apresentar as telas de paradas e manifesto com dados reais, nem executar corretamente as etapas de preparação operacional já previstas no app.

## 4. Ambiente em execução e dados disponíveis

Containers observados:

- `rotalog-demo-api-1`: API publicada em `127.0.0.1:8000`.
- `rotalog-demo-worker-1`: worker da demonstração.
- `rotalog-demo-db-1`: PostgreSQL com PostGIS, sem porta publicada no host.

Resultados das consultas de leitura:

| Verificação | Resultado |
|---|---|
| `GET /health` | HTTP 200, `{"status":"ok"}` |
| Swagger e OpenAPI | Disponíveis |
| Operações HTTP publicadas no OpenAPI | 48 |
| Motoristas ativos no banco | 1 |
| Pedidos em `supplier_order` | 0 |
| Rotas em `delivery_route` | 0 |
| Pontos em `driver_location_point` | 0 |
| Consulta de rotas e de `/me` sem autenticação | HTTP 401 |

As quantidades foram obtidas em transação SQL `READ ONLY`. Não houve criação de pedidos, login, atribuição de rota ou envio de localização.

Isso significa que uma listagem vazia, nesse ambiente, pode ser o resultado correto. Será necessário preparar dados sintéticos de ponta a ponta para validar a futura integração. O healthcheck, isoladamente, não comprova funcionamento de todos os fluxos de negócio.

## 5. O que já existe no backend

Todos os endpoints desta seção usam o prefixo `/api/v1`.

### 5.1 Identidade e sessão

Existem login, refresh, logout, logout de todas as sessões, consulta de usuário, consulta/revogação de sessões e troca de senha.

- O login recebe formulário `application/x-www-form-urlencoded`, com `username` correspondente ao e-mail e `password`.
- O modo `client_type=native`, padrão, retorna access token e refresh token no JSON e é o modo destinado ao Android.
- O README informa access token de 10 minutos e sessão/refresh de 30 dias, com rotação do refresh.
- O cliente precisa serializar renovações: a reutilização de um refresh antigo revoga a família de tokens.
- `GET /me` retorna usuário, papel, organizações e capacidades.
- `X-Organization-Id` seleciona um vínculo autorizado; as operações revalidam sessão e vínculo.
- O cadastro público atual aceita somente `BUYER_ADMIN` e `SUPPLIER_OPERATOR`, com identidade sintética. Não cadastra `DRIVER`.

Há conta sintética de motorista no seed: `motorista@demo.rotalog.invalid`. A senha depende da configuração local; não foi consultada nem incluída neste documento.

### 5.2 Rotas e localização

| Método e caminho | Comportamento atual |
|---|---|
| `POST /supplier-orders/{order_id}/delivery-route` | Fornecedor ou administrador cria a rota para um pedido `READY_TO_DISPATCH` e informa `driver_id` e `expected_version` do pedido. |
| `GET /driver/delivery-routes` | Retorna as rotas do motorista autenticado nos estados `ASSIGNED` e `IN_PROGRESS`, ordenadas pela criação. Não oferece paginação ou filtro no contrato atual. |
| `POST /driver/delivery-routes/{route_id}/start` | Exige motorista responsável, versão correta e estado `ASSIGNED`; muda para `IN_PROGRESS`. |
| `POST /driver/delivery-routes/{route_id}/complete` | Exige motorista responsável, versão correta e estado `IN_PROGRESS`; muda para `COMPLETED`. |
| `POST /driver/delivery-routes/{route_id}/locations` | Registra ponto GPS durante `IN_PROGRESS`, com `Idempotency-Key` obrigatório. |
| `GET /delivery-routes/{route_id}/location` | Retorna a última localização; motorista responsável e demais papéis autorizados têm acesso conforme vínculo com a rota/pedido. |

O contrato `DeliveryRouteResponse` contém:

```text
id
supplier_order_id
driver_id
state
version
last_latitude
last_longitude
last_accuracy_meters
last_location_at
is_synthetic
```

Não contém paradas, manifesto, nome/código operacional da rota, data operacional, endereços, contatos ou eventos de aceite/recebimento.

### 5.3 Regras existentes no rastreamento

- Latitude, longitude, precisão e horário são validados.
- `recorded_at` deve incluir fuso horário.
- São rejeitados pontos com mais de um dia de idade ou mais de cinco minutos no futuro.
- Uma localização com mais de dois minutos é sinalizada como desatualizada na consulta.
- Pontos antigos aceitos não substituem uma última posição com horário mais recente.
- Os pontos são persistidos com representação geográfica PostGIS.
- O banco mantém histórico de pontos, mas a API atual expõe somente a última localização, sem endpoint de trilha completa.
- O rastreamento faz parte da demonstração e retorna `is_synthetic: true`.

Isso não fornece cálculo de percurso pelas ruas, otimização de sequência, tempo estimado ou mapa com destinos.

## 6. Fluxo operacional que o mobile já representa

O aplicativo é uma demonstração local em Kotlin e Jetpack Compose. O fluxo existente é:

```text
Rota atribuída
  → aceite explícito do motorista
  → conferência do manifesto
  → declaração global de conferência
  → confirmação explícita do recebimento
  → confirmação explícita do início
  → consulta da rota ativa e das paradas
```

Regras que precisam permanecer coerentes na futura integração:

1. Aceitar, receber e iniciar são ações independentes.
2. O recebimento depende de aceite, manifesto conferido e ausência de divergências pendentes.
3. Registrar ou resolver divergência desmarca toda a declaração global.
4. Resolver divergência exige nova conferência de toda a carga, incluindo o produto afetado.
5. Receber a carga não inicia a rota; conclui somente a etapa de coleta correspondente.
6. O início depende de aceite, recebimento e ausência de divergências pendentes.
7. Consultar uma etapa anterior não desfaz operações confirmadas.
8. Selecionar ou abrir uma parada não muda seu estado operacional.
9. Depois do recebimento, a carga fica disponível para consulta; o app atual bloqueia alterações de conferência e divergências nessa etapa.
10. O checklist legado de veículo/preparação não participa do fluxo e não deve ser reintroduzido como bloqueio.
11. Estado operacional, estado de envio e estado de salvamento de divergência são conceitos separados.
12. Confirmação simulada nunca deve ser apresentada como resposta do servidor.

Os modelos atuais têm `ASSIGNED`, `ACCEPTED`, `ACTIVE` e `COMPLETED`, além de paradas de coleta, entrega e retorno. Não há ainda execução real de tentativa/resultado de entrega no app. Essa parte precisará ser desenvolvida nos dois projetos, quando entrar no escopo.

## 7. Lacunas para atender ao aplicativo

| Área | Lacuna constatada | Consequência |
|---|---|---|
| Estrutura de rota | Rota vinculada a um único pedido; não há itinerário com paradas. | Não representa diretamente a rota com múltiplos destinos do Android. |
| Detalhe de rota | Não existe consulta individual completa. | Falta recuperar o conjunto operacional por ID, inclusive após encerramento. |
| Paradas | Não há modelo/contrato de parada, sequência, tipo e estado. | Próxima parada, lista, detalhes e progresso não podem vir do servidor. |
| Dados do destino | A rota não retorna endereço, referência, janela, responsável, telefone ou instruções. | As telas atuais dependeriam de conteúdo fictício. |
| Aceite do motorista | Não existe ação nem registro desse aceite. | Não é possível sincronizar a primeira etapa do app. |
| Manifesto | Não existe manifesto operacional acessível ao motorista. | A carga não pode ser conferida com base no servidor. |
| Declaração global | Não existe registro/validação da declaração. | Falta vincular a conferência à carga efetivamente recebida. |
| Divergências | Não há registro, consulta ou resolução. | Falta persistência e bloqueio de recebimento no servidor. |
| Recebimento | Não existe comando independente. | Não há confirmação de transferência da carga ao motorista. |
| Pré-requisitos do início | O servidor aceita início direto de `ASSIGNED`. | A regra do servidor é menos restritiva que a regra do app. |
| Execução da entrega | Não há tentativa, resultado, quantidade entregue, recusa ou ocorrência operacional. | A operação após a partida permanece incompleta. |
| Encerramento operacional | `complete` muda o estado da rota sem verificar entregas ou retorno. | Não comprova que o serviço foi concluído. |
| Histórico | A listagem exclui concluídas e não há consulta histórica completa. | A aba Histórico não consegue substituir os exemplos locais. |
| Mapa | Não há coordenadas de paradas nem geometria do itinerário. | Última localização do motorista não basta para desenhar a rota. |
| Perfil | `/me` não retorna matrícula, telefone ou veículo exibidos no exemplo Android. | Será necessário ajustar a interface ou fornecer esses dados. |

Nem toda lacuna exige um endpoint separado. Por exemplo, um detalhe de rota pode incluir paradas e manifesto; a declaração global pode ser enviada junto ao recebimento. O requisito é preservar dados, autorização e regras, com um contrato claro.

## 8. Diferenças de contrato e comportamentos que podem causar integração incorreta

### 8.1 Pedido de fornecedor não é itinerário de entregas

`DeliveryRoute.supplier_order_id` tem unicidade. A estrutura atual associa uma rota a um pedido e impede uma segunda rota para esse mesmo pedido.

O mobile reúne coleta, várias entregas e retorno na mesma rota. É necessário decidir a relação entre rota, parada, pedido e eventual remessa/fulfillment. Também deve ser decidido se pedidos podem ser divididos entre rotas e como tratar reentregas.

Não basta concatenar rotas de pedidos no Android: isso não cria sequência operacional, responsabilidades ou consistência no servidor.

### 8.2 O aceite comercial existente não atende ao aceite do motorista

`POST /supplier-orders/{id}/acceptance` é a decisão do fornecedor sobre as linhas do pedido. Não é aceite de rota pelo motorista e não deve ser reaproveitado com esse significado.

Da mesma forma, o pedido estar `READY_TO_DISPATCH` comprova prontidão no recorte atual; não comprova que o motorista conferiu ou recebeu a carga.

### 8.3 Os produtos do pedido não estão disponíveis ao motorista

Embora o pedido possua produtos e quantidades, o serviço de detalhe de `supplier-orders` permite comprador/fornecedor e rejeita motorista. A rota também não inclui esses itens.

É necessário oferecer uma visão operacional autorizada ao motorista responsável. Abrir acesso genérico aos pedidos comerciais não é necessário para resolver essa lacuna; o contrato pode expor somente os dados usados na coleta e entrega.

### 8.4 Encerrar rastreamento não equivale a comprovar entrega

O serviço `complete_route` apenas verifica versão e estado, altera para `COMPLETED` e incrementa a versão. Não verifica paradas, volumes, comprovantes, divergências ou retorno. Não atualiza o pedido como entregue.

O README do backend também esclarece que entrega, prova e retorno continuam fora do escopo atual. Portanto, o botão/estado futuro do app não deve tratar esse endpoint como confirmação completa de entrega sem uma revisão do contrato.

### 8.5 Existem quantidades no contrato que ainda são valores fixos

O retorno do pedido expõe campos como `dispatched_quantity`, `delivered_quantity`, `buyer_accepted_quantity`, `delivery_refused_quantity`, `missing_quantity`, `returning_quantity` e `return_reconciled_quantity`.

No serviço atual de montagem da resposta, esses campos são devolvidos como zero fixo. Sua presença no OpenAPI não significa que exista fluxo para atualizá-los. Há também quantidade despachada no modelo de linha de fulfillment, mas não foi encontrado um fluxo de despacho completo exposto que sustente a operação do mobile.

### 8.6 GPS e comandos operacionais compartilham a versão

Cada novo ponto GPS incrementa `DeliveryRoute.version`. A resposta do ponto não retorna a nova versão da rota. O comando de conclusão exige `expected_version`.

Exemplo de possível conflito, deduzido do código:

1. O app consulta a rota na versão 4.
2. O rastreamento envia um ponto e a rota passa à versão 5.
3. O usuário tenta concluir com a versão 4 e recebe `VERSION_CONFLICT`.

Mesmo relendo a rota antes da conclusão, outro ponto pode chegar entre a consulta e o comando. É necessário decidir se posição e operação terão versões independentes, se a posição deixará de alterar a versão operacional ou qual coordenação será adotada. Este conflito foi identificado por análise do código, sem reprodução mutável na API.

Início e conclusão também não possuem `Idempotency-Key`. Repetir o mesmo comando após perda de resposta não retorna automaticamente o sucesso anterior: pode gerar conflito de versão/estado. A futura política de repetição e reconciliação precisa ser explícita.

### 8.7 Estados e cardinalidade diferem entre os projetos

- Backend: `ASSIGNED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`.
- Mobile: `ASSIGNED`, `ACCEPTED`, `ACTIVE`, `COMPLETED`, com recebimento separado.
- `IN_PROGRESS` pode ser convertido para `ACTIVE`, mas falta representar o aceite e o recebimento.
- `CANCELLED` é permitido pelo banco; não foi encontrado endpoint de cancelamento de rota.
- O backend lista várias rotas para o motorista. O repositório do app mantém uma rota atual.
- Não foi encontrada regra no serviço que impeça iniciar duas rotas atribuídas ao mesmo motorista. A política desejada precisa ser definida.

### 8.8 Login e provisionamento de motorista precisam de decisão

O app atual recebe um identificador, matrícula ou telefone, sem senha, e informa que o acesso real está indisponível. O backend autentica e-mail e senha.

É necessário escolher entre adaptar o app ao login atual ou criar um fluxo de acesso de motorista no backend. Aceitar um identificador não vazio e criar uma sessão local não atende ao requisito de autenticação.

O cadastro público também não cria motorista, e não foi encontrado endpoint administrativo para listar/criar motoristas e apoiar a seleção de `driver_id`. O seed atende apenas ao cenário sintético conhecido.

### 8.9 Capacidades publicadas não refletem o uso logístico

O perfil retorna `capabilities`, mas a tabela do serviço de identidade define uma lista vazia para `DRIVER`. Os endpoints logísticos autorizam pelo papel e vínculo, por isso a lista vazia não significa que estejam bloqueados.

Se o Android decidir quais ações exibir usando apenas essa lista, poderá ocultar funcionalidades disponíveis. É necessário alinhar o significado das capacidades ou definir outra fonte explícita de ações permitidas.

### 8.10 Quantidades e identificadores precisam de semântica comum

O mobile chama a soma das quantidades de “volumes” e usa `productId` para a conferência. O backend possui quantidades de pedido e atributos de embalagem, peso e volume físico.

É necessário definir se uma quantidade representa caixa, fardo, embalagem comercial ou unidade interna. Não converter `quantity` em volumes sem essa definição. Também é preciso distinguir o mesmo produto em pedidos, paradas, embalagens ou lotes diferentes; apenas `productId` pode ser insuficiente para identificar uma divergência.

Uma identidade de linha de manifesto e sua associação às linhas de pedido/remessa devem ser avaliadas. Totais e progresso devem ser derivados desses dados, sem somar coleta e entrega como duas cargas distintas.

## 9. O que existe além do que o aplicativo usa hoje

### 9.1 No backend

- Catálogo, ofertas, depósitos, estoque, capacidade, checkout, compras, separação e financeiro simulado atendem a outros papéis do marketplace. Não são endpoints a remover por não aparecerem no Android.
- Envio de GPS e consulta de última localização já existem, enquanto o app usa mapa ilustrativo sem GPS.
- O encerramento global de rota já existe, mas o app ainda não oferece execução real de entregas ou encerramento.
- Gestão de sessões e troca de senha estão disponíveis para uma integração futura.

### 9.2 No mobile

- Os cenários demonstrativos e seletores manuais de envio são ferramentas da demonstração; não precisam virar endpoints de produção.
- O checklist legado permanece no código para compatibilidade, mas não deve gerar novas exigências operacionais no backend.
- A consulta de etapa, seleção de marcador, rolagem e expansão de detalhes são estados de interface. Não precisam alterar o estado operacional no servidor.
- Tema pode continuar local; não é uma dependência para integrar rotas.
- O exemplo inclui higiene e limpeza, enquanto o catálogo atual do backend se restringe a alimentos secos industrializados. Os dados de demonstração precisam ser alinhados ao escopo acordado.

## 10. Dependências de integração no Android

Além de completar o backend, será necessário trabalho no aplicativo:

- Substituir/injetar uma implementação remota do contrato de rotas. `AppContainer` instancia hoje `DemoRouteRepository` diretamente.
- Implementar cliente HTTP, DTOs e conversão para modelos de domínio.
- Implementar autenticação, manutenção de sessão, refresh serializado e tratamento de expiração.
- Declarar permissão de internet e configurar acesso de desenvolvimento.
- Tratar carregamento, ausência real de rota, falha de rede, sessão expirada, falta de permissão e conflito de versão.
- Separar confirmação do servidor de atualização otimista, envio pendente e erro.
- Definir persistência durável de dados e comandos caso operação offline entre no escopo. Hoje apenas o tema tem persistência durável; não existe fila offline real.
- Adaptar o contrato que mantém uma rota atual se múltiplas rotas por motorista forem permitidas.
- Implementar GPS e os requisitos Android correspondentes somente se o rastreamento entrar na entrega.

Para o ambiente local, a API está publicada apenas no loopback do computador. `127.0.0.1` dentro do Android aponta para o próprio dispositivo/emulador. Será necessário definir o acesso de desenvolvimento, por exemplo com encaminhamento de porta via ADB ou configuração apropriada de rede. Não é necessário publicar a API externamente para resolver o desenvolvimento local.

O acesso é HTTP local. A configuração de desenvolvimento do Android deve considerar isso, sem transformar uma exceção local em configuração ampla de produção.

## 11. Proposta de capacidades e endpoints para discussão

**Os caminhos abaixo são sugestões, não endpoints existentes nem contrato canônico aprovado.** O responsável pode agrupá-los ou adotar outra nomenclatura, mantendo as regras observáveis.

| Prioridade | Capacidade | Sugestão de contrato |
|---|---|---|
| P0 | Listar rotas e recuperar detalhe autorizado | Evoluir `GET /driver/delivery-routes` e adicionar `GET /driver/delivery-routes/{route_id}`. |
| P0 | Consultar paradas e manifesto | Incluir no detalhe ou oferecer recursos `/stops` e `/manifest`. |
| P0 | Registrar aceite do motorista | `POST /driver/delivery-routes/{route_id}/acceptance`. |
| P0 | Registrar/consultar divergências | `POST` e `GET /driver/delivery-routes/{route_id}/cargo-issues`, caso não venham no detalhe. |
| P0 | Resolver divergência com rastreabilidade | `POST /driver/delivery-routes/{route_id}/cargo-issues/{issue_id}/resolve`. Definir o papel autorizado. |
| P0 | Confirmar recebimento com declaração global | `POST /driver/delivery-routes/{route_id}/receipt`, vinculado à versão do manifesto. |
| P0 | Iniciar somente com pré-requisitos cumpridos | Evoluir o `/start` existente. |
| P1 | Histórico recuperável | Filtros/paginação para estados finais e detalhe acessível após conclusão. |
| P1 | Registrar execução por parada | Contrato de tentativa e resultado, por exemplo sob `/stops/{stop_id}/attempts`. |
| P1 | Encerrar operação com validações | Revisar `/complete` conforme resultados, pendências e política de retorno. |
| Conforme escopo | Provas, devoluções, reentrega, cancelamento e reatribuição | Definir recursos e responsáveis após a decisão de produto. |
| Conforme escopo | Provisionamento e seleção de motorista | Contrato administrativo/operacional ou mecanismo existente aprovado. |

P0 significa necessário para conectar fielmente o fluxo atual de aceite, recebimento e início. P1 amplia a operação após a partida e a consulta histórica; não significa que essas funcionalidades já existam completas no Android.

Um detalhe operacional suficiente deve permitir obter: identificação e data da rota, estado e versões, motorista responsável, sequência de paradas, tipo/estado de cada parada, destinatário/origem, endereço, janela, contatos, instruções, itens/quantidades/unidades, manifesto, divergências e registros de aceite/recebimento/início. Campos opcionais, ausência de contato e formato de data/hora devem ser documentados.

Para o recebimento, uma alternativa é enviar a declaração afirmativa e a versão do manifesto no próprio comando e validá-las atomicamente. Não é obrigatório persistir cada toque no checkbox em um endpoint separado. A solução precisa impedir recebimento de uma carga diferente daquela conferida e invalidar a declaração após divergência ou mudança relevante do manifesto.

Recomenda-se que os comandos retornem estado/versão atualizados e que erros tenham códigos estáveis. O envelope já existente `error.code/message/details/request_id` pode ser preservado.

## 12. Decisões que o responsável precisa fechar

1. Uma rota pode reunir quais pedidos, fornecedores, depósitos e destinatários?
2. Um pedido pode ser dividido em várias rotas/remessas? Como tratar reentrega?
3. Quantas rotas podem estar em andamento para um motorista e como escolher a rota atual?
4. O acesso continuará por e-mail/senha ou haverá autenticação por matrícula/telefone? Quem provisiona motoristas?
5. Qual é a unidade operacional da carga e como cada linha do manifesto é identificada?
6. Quem pode resolver uma divergência: motorista, expedição ou operação? Qual evidência e motivo são exigidos?
7. Quando o manifesto fica imutável? Como alterações após conferência afetam o recebimento?
8. Quais condições encerram uma parada e quais encerram a rota? O retorno à base é obrigatório em todos os casos?
9. Como serão tratadas recusa, entrega parcial, ausência, devolução e reatribuição?
10. O primeiro incremento terá operação offline e GPS em segundo plano ou somente operação conectada?
11. Como compatibilizar o `/complete` atual e eventuais consumidores existentes com a nova semântica operacional?

Uma IA não deve resolver essas questões silenciosamente criando regras novas. Deve explicitar as hipóteses e submeter ao responsável as decisões que alterem o comportamento do produto.

## 13. Critérios de aceite para a implementação futura

Estes critérios orientam testes; não são resultados já executados.

### 13.1 Autorização e dados

- Motorista consulta e altera somente suas rotas autorizadas.
- Outro motorista não acessa manifesto, paradas, divergências ou dados de contato de rota alheia.
- O detalhe oferece os dados necessários às telas, inclusive quando a rota está concluída e é consultada no histórico.
- Lista vazia é diferenciada de erro, falta de permissão e sessão expirada.

### 13.2 Aceite, recebimento e início

- Aceitar não recebe carga nem inicia a rota.
- Receber sem aceite é rejeitado.
- Receber sem declaração válida ou com manifesto vazio é rejeitado.
- Divergência pendente bloqueia o recebimento.
- Registrar ou resolver divergência invalida a declaração global anterior.
- Após resolução, uma nova conferência global é exigida.
- Mudança relevante da carga invalida a conferência da versão anterior.
- Receber conclui somente a coleta correspondente e não registra entregas como realizadas.
- Iniciar sem recebimento é rejeitado.
- Nenhuma operação exige o checklist legado.
- Consultar etapa ou selecionar parada não altera o progresso operacional.

### 13.3 Concorrência e envio

- Comandos com versão desatualizada têm comportamento documentado e estado recuperável.
- Duplo toque, timeout e repetição não duplicam recebimento, divergência ou resultado de entrega.
- O envio simultâneo de GPS não impede indefinidamente comandos operacionais.
- Fechar/reabrir o app permite reconciliar o resultado real de uma operação cuja resposta foi perdida.
- Uma resposta HTTP de sucesso não é simulada a partir de conectividade, temporizador ou seletor manual.

### 13.4 Operação após a partida, se incluída no escopo

- Resultado de uma parada atualiza quantidades e progresso de forma consistente.
- Finalizar a rota respeita a política de paradas pendentes, ocorrências e retorno.
- Conclusão da rota, conclusão da entrega e encerramento do rastreamento têm significado documentado.
- Totais e histórico vêm dos registros operacionais, sem valores fictícios ou campos fixos apresentados como resultados reais.

## 14. Sequência sugerida de trabalho

1. Aprovar as decisões de domínio e o escopo do primeiro incremento.
2. Definir o contrato OpenAPI de detalhe de rota, paradas, manifesto e comandos independentes.
3. Implementar no backend as regras e permissões, com migrações que preservem os dados existentes.
4. Preparar um cenário sintético que percorra pedido pago, aceite comercial, separação, prontidão e atribuição ao motorista.
5. Validar os contratos por HTTP antes de conectar as telas.
6. Integrar no Android identidade, listagem/detalhe e o fluxo de aceite → recebimento → início.
7. Integrar histórico e, em incrementos próprios, execução de entrega, provas, retorno e rastreamento conforme escopo aprovado.
8. Atualizar README, Swagger, exemplos, coleção Postman e documentos de compatibilidade.

No Android, as instruções do projeto exigem TDD: teste comportamental falhando antes da implementação, implementação mínima e refatoração com testes verdes. Correções precisam manter teste de regressão. A futura entrega deve registrar o red observado e as verificações executadas. Durante os incrementos, usar as suítes afetadas; ao finalizar, executar a validação completa prevista em `AGENTS.md` e `docs/TESTING.md`, incluindo instrumentados para mudanças de interface/fluxo.

Para o backend, verificar suas próprias instruções e infraestrutura de testes antes de implementar; não executar testes destrutivos sobre o banco da demonstração. O README exige banco de testes dedicado. Recomenda-se cobrir as mesmas regras de negócio por testes de serviço/API e integração com PostgreSQL.

## 15. Documentação e testes existentes: cuidados de interpretação

Alguns documentos do backend, como o guia Swagger, a leitura da coleção Postman e registros de compatibilidade/validação, ainda afirmam que motorista ou tracking não existem. Essa afirmação está desatualizada em relação ao router, serviço, README atual e OpenAPI observado.

Para esta análise, a existência dos endpoints foi confirmada no OpenAPI em execução e no código correspondente do container. O comportamento foi lido nos serviços. Documentos históricos não devem fazer a IA remover ou recriar o rastreamento que já existe.

Existe `tests/test_tracking.py` com um fluxo de criação, listagem, início, ponto GPS, repetição idempotente, consulta e conclusão. Esse teste foi lido, mas não executado. Ele não demonstra que aceite do motorista, recebimento, paradas ou provas existam. Os critérios de aceite propostos acima precisam de cobertura própria quando implementados.

## 16. Evidências e arquivos para a IA consultar

Os caminhos abaixo são relativos à raiz de cada repositório.

| Repositório / arquivo | O que verificar |
|---|---|
| Backend — `src/rotalog/logistics/router.py` | Seis endpoints logísticos atuais e seus contratos. |
| Backend — `src/rotalog/logistics/schemas.py` | Campos retornados pela rota e pelos pontos GPS. |
| Backend — `src/rotalog/logistics/models.py` | Estados, relação única com pedido e persistência de localização. |
| Backend — `src/rotalog/logistics/service.py` | Autorização por rota, filtro da listagem, início, conclusão e versão incrementada por GPS. |
| Backend — `src/rotalog/ordering/service.py` | `_order` limita acesso ao pedido; `_order_response` retorna quantidades operacionais fixas; aceite é comercial. |
| Backend — `src/rotalog/ordering/schemas.py` e `models.py` | Pedidos, itens e fulfillments já existentes. |
| Backend — `src/rotalog/identity/router.py` e `schemas.py` | Login por formulário, tokens e restrições do cadastro. |
| Backend — `src/rotalog/identity/service.py` | Sessão, autorização e capacidades vazias de `DRIVER`. |
| Backend — `src/rotalog/demo/seed.py` | Contas sintéticas e preparação inicial do ambiente. |
| Backend — `src/rotalog/core/contracts.py` | `expected_version`, `VERSION_CONFLICT` e contratos comuns. |
| Backend — `tests/test_tracking.py` | Cobertura de tracking existente, sem presumir aprovação nesta análise. |
| Backend — `README.md`, `compose.yaml` | Escopo da demo, execução, autenticação, rede e banco dedicado para testes. |
| Mobile — `AGENTS.md` e `README.md` | Regras operacionais, arquitetura, TDD e limitações atuais. |
| Mobile — `app/src/main/java/com/rotalog/delivery/model/Models.kt` | Modelos de rota, parada, manifesto, divergência, totais e pré-requisitos. |
| Mobile — `app/src/main/java/com/rotalog/delivery/data/Contracts.kt` | Ações esperadas pelo repositório do aplicativo. |
| Mobile — `app/src/main/java/com/rotalog/delivery/data/DemoRouteRepository.kt` | Comportamento local existente e fixtures. |
| Mobile — `app/src/main/java/com/rotalog/delivery/AppContainer.kt` | Instanciação atual do repositório demonstrativo. |
| Mobile — `app/src/main/java/com/rotalog/delivery/login/LoginViewModel.kt` | Identificador local sem autenticação real. |
| Mobile — `app/src/main/java/com/rotalog/delivery/routes/` | ViewModel, etapas, telas, mapa demonstrativo e consulta de paradas. |
| Mobile — `app/src/main/AndroidManifest.xml` e `app/build.gradle.kts` | Configuração e dependências atuais, sem integração HTTP implementada. |
| Mobile — `docs/ACCESS-CARGO-ROUTES-TDD.md`, `docs/ACCESS-CARGO-UX-VALIDATION.md` e `docs/TESTING.md` | Histórico das regras de carga e validação do app. |

## 17. Limites desta entrega

Foi realizada análise de código, contratos publicados e estado local do ambiente. Não foram implementados endpoints, autenticação, integração Android, migrações ou cenários de dados. Não foram modificadas credenciais, infraestrutura ou configurações.

Não foram executados login, comandos operacionais, testes automatizados, build, lint ou validação visual em emulador. As descrições de autorização e transições protegidas são constatações no código, não resultados de um teste autenticado de ponta a ponta.

Este documento é o único arquivo criado nesta entrega. Os caminhos propostos, prioridades e critérios de aceite devem orientar o alinhamento técnico antes da implementação.
