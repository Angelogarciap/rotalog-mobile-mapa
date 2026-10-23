# Acesso do motorista — ciclos da frente Login

Base `de457d3`; trabalho em `feature/ux-login-identificador`, sem commit. Versão 1.2 / código 3 preservada. Sem novas dependências.

## Mudança

O acesso usa um identificador livre (código, matrícula ou telefone), sem validação de cadastro. Apenas espaços em branco são rejeitados. Nenhum dado é enviado e nenhuma sessão é criada pelo envio. O aviso de indisponibilidade tem região acessível `Polite`. A exploração da demonstração continua separada.

O identificador usa a nova chave `loginIdentifier` no `SavedStateHandle`, com restauração da tarefa e sem persistência durável; `loginEmail` antigo não é convertido. A tela reutiliza Inter, paletas, cards, marca e largura máxima de 420 dp, com rolagem e teclado. Retirados senha, recuperação e visibilidade.

## Red

1. Antes de alterar produção, adicionado `identifiersNeedNoPasswordOrEmailSyntax` ao teste unitário existente, usando provisoriamente o setter antigo para compilar sobre a API real. Execução filtrada falhou por asserção: esperado `Acesso indisponível nesta versão. Nenhum dado foi enviado.`, observado `null`, pois a validação anterior exigia e-mail e senha.
2. Antes de alterar produção, adicionado `driverAccessHasOneIdentifierFieldAndNoPasswordControls` aos instrumentados. Execução isolada compilou e falhou por `assertExists`: nenhum nó com `Acesso do motorista`, confirmando a ausência da nova composição.

Logs e XMLs reais ficam em `app/build/review/access-login/`, fora do Git. Falhas de ambiente/compilação não foram contadas como red.

## Green e refactor

Implementados o estado mínimo de identificador, o formulário e a ligação em `RotalogApp`. O teste unitário original foi migrado para `setIdentifier` após o red; suas asserções de resultado foram conservadas. Os testes das regras antigas de e-mail/senha/recuperação foram deliberadamente substituídos pelas regras aprovadas, sem preservar requisitos removidos.

Cobertura complementar: vazio/espaços, matrícula alfanumérica/numérica/telefone, edição limpa feedback, restauração da tarefa, descarte do e-mail legado, demonstração separada, um campo único, ausência dos controles antigos, ação Done, aviso acessível, recriação e último controle alcançável. Esses casos complementares não são apresentados como ciclos red independentes.

Refactor: captura usa os helpers compartilhados existentes, removendo a implementação duplicada do teste anterior. O componente da marca foi preservado. Suíte Login repetida após essa consolidação.

- Unitários Login: **5 aprovados**.
- Instrumentados Login, 360 × 640 dp, fonte 100%: **4 aprovados**, incluindo revisão nos temas claro/escuro e teclado.
- Instrumentados Login, 360 × 640 dp, fonte 200%: **4 aprovados**, incluindo temas claro/escuro, teclado, aviso acessível e acesso ao último controle. Fonte restaurada para 100% ao liberar o emulador.

As capturas são produzidas em `Download/RotaLogUnifiedReview` com prefixos `LIGHT-driver-*` e `DARK-driver-*`, incluindo páginas completas, teclado, erro e indisponibilidade. A integração final consolida APK, galeria, lint e regressão completa.

Limites: autenticação real indisponível por ausência de contrato/backend; ícones de conectividade, selos de usuário e permissões do protótipo ficam fora do escopo aprovado. Referências Stitch seguem os identificadores do plano; esta frente não obteve acesso aos arquivos do Stitch nesta sessão.
