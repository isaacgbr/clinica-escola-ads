# Backend — Clínica Escola

API desenvolvida por Isaac e Aaron, com health check, cadastro e autenticação
por sessões persistentes no PostgreSQL.

## Tecnologias

- **Node.js:** ambiente de execução da API.
- **TypeScript:** tipagem estática e compilação para JavaScript.
- **Express:** servidor HTTP, rotas e middlewares.
- **Prisma:** cliente de acesso a dados e mapeamento do banco existente.
- **PostgreSQL:** banco relacional já existente, acessado pelo adaptador `@prisma/adapter-pg`.
- **dotenv:** carregamento das variáveis de um `.env` local.
- **express-rate-limit:** limitação de tentativas de login em memória na instância local.

## Instalação e execução

Requer Node.js compatível com `^22.18.0 || ^24.11.0 || >=26.0.0` e npm.
Execute os comandos abaixo a partir de `backend/`:

```sh
npm ci
npm run dev
```

O modo de desenvolvimento usa `tsx watch`, reiniciando a API após alterações.
Por padrão, o endereço é `http://localhost:3000`.

Para compilar e executar o JavaScript gerado:

```sh
npm run build
npm start
```

O build gera o cliente Prisma e compila o TypeScript para `dist/`.
`npm start` executa `dist/src/server.js` e depende de um build prévio.

## Variáveis de ambiente

Use `backend/.env.example` como referência para um `.env` local em `backend/`
ou configure as variáveis diretamente no ambiente de execução.

| Variável | Uso |
| --- | --- |
| `PORT` | Porta HTTP, entre 1 e 65535. Se ausente ou vazia, utiliza 3000. |
| `DATABASE_URL` | URL real de conexão com o PostgreSQL existente. Necessária para acessar o banco e realizar introspecção. |
| `NODE_ENV` | Use `production` no ambiente de produção. |
| `AUTH_ORIGIN` | Origem exata autorizada para autenticação, sem caminho ou barra final. No desenvolvimento, o padrão é `http://localhost:PORT` (porta 3000 se não definida). Em produção é obrigatória e deve usar HTTPS. |

O exemplo contém somente os nomes das variáveis, sem credenciais. Solicite os
parâmetros reais ao responsável pelo banco; não versione o `.env` nem exponha
credenciais. A API e o health check podem ser executados sem `DATABASE_URL`.

## Arquitetura

Fluxo das funcionalidades com acesso a dados:

```text
routes → controllers → services → repositories → Prisma → PostgreSQL
```

| Local | Responsabilidade |
| --- | --- |
| `src/routes/` | Associar métodos e caminhos HTTP aos controllers. |
| `src/controllers/` | Receber requisições e construir respostas HTTP. |
| `src/services/` | Aplicar regras de cadastro, autenticação e validade das sessões. |
| `src/repositories/` | Concentrar acesso aos usuários e sessões via Prisma. |
| `src/middlewares/` | Autenticar sessões, validar CSRF, limitar tentativas e padronizar erros. |
| `src/utils/` | Compartilhar scrypt e leitura restrita do cookie de sessão. |
| `src/config/` | Definir origem autorizada, cookie e prazos de sessão. |
| `src/app.ts` | Configurar Express, leitura de JSON e rotas. |
| `src/server.ts` | Carregar o ambiente, validar a porta e iniciar o servidor HTTP. |
| `prisma/` | Manter o schema usado pelo Prisma. |

O health check usa apenas rota e controller, pois não possui regra de negócio
nem acesso a dados. Novas camadas devem ser utilizadas conforme a necessidade,
sem abstrações prematuras.

## Cadastro de usuários

`POST /usuarios` exige `Content-Type: application/json`, com corpo de até 16 KiB:

```json
{
  "nome": "Maria Silva",
  "email": "maria@example.com",
  "senha": "Senha-exemplo-123!",
  "confirmar_senha": "Senha-exemplo-123!",
  "nascimento": "2000-02-29",
  "telefone": "(81) 99999-9999"
}
```

Todos os campos são obrigatórios e devem ser strings. Campos adicionais são rejeitados.

- Nome: remove espaços nas extremidades, exige conteúdo, limita a 150 caracteres e rejeita NUL (`\u0000`).
- Email: remove espaços nas extremidades, converte para minúsculas, valida o formato e limita a 150 caracteres.
- Senha: de 8 a 128 caracteres, até 512 bytes, sem remover espaços; confirmação idêntica.
- Nascimento: data real entre `0001-01-01` e o dia atual em `America/Sao_Paulo`, no formato `YYYY-MM-DD`.
- Telefone: remove espaços, parênteses, pontos e hífens; exige 10 ou 11 dígitos com DDD, sem código internacional.

Sucesso: HTTP 201, com `{"usuario":{"id":"1","nome":"Maria Silva"}}`.
O ID é uma string por corresponder a um `BigInt` no banco. A senha é armazenada
como hash scrypt com salt aleatório; senha, confirmação e hash não são retornados.

Erros usam `{"erro":{"codigo":"...","mensagem":"..."}}`, incluindo `campos`
quando há falhas de validação:

| HTTP | Código | Situação |
| --- | --- | --- |
| 400 | `DADOS_INVALIDOS` | Campos, tipo de conteúdo ou corpo JSON inválidos/excessivos. |
| 409 | `EMAIL_JA_CADASTRADO` | Email já existente, inclusive com diferença de maiúsculas ou cadastro concorrente. |
| 500 | `ERRO_INTERNO` | Falha inesperada, sem detalhes internos na resposta. |

## Login e sessões

As rotas de autenticação usam `Cache-Control: no-store`. Para os dois POSTs,
envie `Content-Type: application/json`, `X-Requested-With: XMLHttpRequest`
e `Origin` igual a `AUTH_ORIGIN`. O navegador envia `Origin` automaticamente.
Origens ausentes, `null`, diferentes ou marcadas como `cross-site` são rejeitadas.
O login recebe somente `email` e `senha`, com os mesmos limites do cadastro;
a senha é preservada e o email é normalizado.

| Rota | Resultado |
| --- | --- |
| `POST /auth/login` | 200 com `{"usuario":{"id":"1","nome":"Maria Silva"}}` e cookie de sessão. |
| `GET /auth/me` | 200 com o mesmo formato de usuário, mediante cookie válido. |
| `POST /auth/logout` | 204; revoga a sessão autenticada e remove o cookie. Envie `{}` como corpo. |

Erros mantêm o envelope `erro` do cadastro:

| HTTP | Código | Situação |
| --- | --- | --- |
| 400 | `DADOS_INVALIDOS` | Corpo, campos ou tipo de conteúdo inválidos. |
| 401 | `CREDENCIAIS_INVALIDAS` | Email inexistente ou senha incorreta, com a mesma mensagem. |
| 401 | `NAO_AUTENTICADO` | Cookie ausente/inválido ou sessão expirada/revogada. |
| 403 | `ORIGEM_NAO_PERMITIDA` | Falha na verificação de origem ou cabeçalho CSRF. |
| 429 | `MUITAS_TENTATIVAS` | Limite de login atingido; respeite `Retry-After`. |
| 500 | `ERRO_INTERNO` | Falha inesperada, sem detalhes internos. |

O cadastro e o login compartilham scrypt com salt aleatório. A verificação usa
comparação em tempo constante e também deriva uma chave quando o email não existe
ou o hash armazenado é inválido. A consulta parametrizada de credenciais usa
igualdade literal em `LOWER(email)`, sem interpretar `%` ou `_` como padrões.

A sessão utiliza identificador aleatório de 256 bits. Somente seu SHA-256 é salvo
no banco; o identificador original é enviado exclusivamente no cookie `HttpOnly`,
`SameSite=Lax`, `Path=/`, sem `Domain`. Em HTTPS, o cookie se chama
`__Host-clinica_sessao` e usa `Secure`. No HTTP local em loopback, usa
`clinica_sessao_local`. A configuração recusa HTTP não local e HTTP em produção.

O prazo absoluto é de 8 horas, com expiração por 30 minutos de inatividade,
sempre conferidos no servidor. Requisições autenticadas renovam a atividade,
sem estender o prazo absoluto. Um novo login substitui o identificador apresentado
e revoga a sessão anterior; sessões de outros dispositivos continuam válidas.
Logout revoga somente a sessão atual. Sessões inválidas resultam em 401 e limpeza
do cookie. Senhas, hashes e identificadores não são incluídos no JSON ou em logs.

Os limites são 5 tentativas por combinação de email/IP e 20 por IP em 15 minutos.
Respostas 401 e bloqueios 429 consomem cota; sucessos não consomem e limpam o
histórico da combinação email/IP, sem limpar as falhas gerais daquele IP.
Emails são normalizados e as chaves combinadas são armazenadas como hash;
IPv6 é agrupado pelo helper da biblioteca. As contagens ficam em memória,
reiniciam com o processo e não são compartilhadas entre instâncias.
Sessões, ao contrário, permanecem no PostgreSQL após reiniciar a API.

O Frontend deve acessar a API pela mesma origem, normalmente via proxy.
Não há CORS permissivo: origem explícita, JSON e cabeçalho customizado são
a proteção CSRF, complementada por `SameSite`. Não enviar credenciais ou cookies
em URLs nem armazenar o identificador de sessão em `localStorage`.
O Express não confia em cabeçalhos de proxy por padrão; qualquer implantação
atrás de proxy precisa revisar `trust proxy`, HTTPS e armazenamento compartilhado
dos limites antes de operar com múltiplas instâncias.

## Validações e testes

```sh
npm run prisma:validate
npm run typecheck
npm run build
npm test
```

`prisma:validate` valida o schema. `typecheck` gera o cliente Prisma e verifica
código e testes com `tsconfig.test.json`, sem emitir JavaScript. O build mantém
`tsconfig.json` e não compila os testes para `dist/`. Esses comandos não alteram
tabelas nem exigem conexão com o banco.

`npm test` executa testes unitários e HTTP locais sem acesso ao banco, incluindo
validações de cadastro/login, scrypt, cookies, CSRF, respostas de erro e os dois
limites de tentativas. Preserva as regressões de nome e datas do cadastro.
`npm run test:integration` exige um banco de teste já preparado com a estrutura
existente, incluindo `sessoes`, e os seguintes requisitos:

- `NODE_ENV=test` e `ALLOW_DB_TESTS=1`.
- PostgreSQL local (loopback): `localhost`, `127.0.0.1` ou `[::1]`.
- Nome do banco em letras minúsculas, números e sublinhados, terminado em `_test` ou `_teste`.
- `DATABASE_URL` com protocolo `postgresql://` ou `postgres://`, sem parâmetros (`?...`) ou fragmento (`#...`).

Para usar o banco `clinica_escola_test`, execute em um terminal PowerShell
dedicado, a partir de `backend/`. Substitua os valores ilustrativos de usuário
e senha pelos dados locais do banco de teste, sem versioná-los ou alterar o `.env`:

```powershell
$env:NODE_ENV = 'test'
$env:ALLOW_DB_TESTS = '1'
$env:DATABASE_URL = 'postgresql://USUARIO_TESTE:SENHA_TESTE@localhost:5432/clinica_escola_test'
npm run test:integration
```

Feche esse terminal ao terminar para descartar as variáveis configuradas nele.
Os testes verificam cadastro, persistência, hash,
duplicidade, concorrência, login correto/incorreto, cookies, reconexão ao banco,
expiração absoluta/inatividade, rotação de identificador, logout, CSRF e bloqueio.
Cria registros identificados por UUID e os remove ao final, incluindo suas sessões.
Não executa migrações. Use um banco destinado a testes.

Com a API em execução, faça `GET http://localhost:3000/health` pelo navegador ou
por um cliente HTTP, ajustando a porta se necessário. Resultado esperado:

```text
HTTP 200
{"status":"ok"}
```

Esse endpoint verifica a disponibilidade da API, sem testar a conexão com o banco.

## Integração com o PostgreSQL

O datasource usa o provider `postgresql`. A configuração em `prisma.config.ts`
lê `DATABASE_URL`; `src/repositories/prisma.ts` fornece `getPrisma()`, que cria e
reutiliza um cliente com `PrismaPg` sob demanda. Sem a variável, essa função
lança um erro ao ser chamada.

O schema mapeia `usuarios`, `recuperacao_senha` e `sessoes`. O cadastro usa os tipos,
limites e valores padrão do banco existente. A unicidade de email depende do
índice SQL em `LOWER(email)`, inclusive para requisições concorrentes; esse
índice de expressão não é representado como `@unique` no modelo Prisma.

Não execute `prisma db push` ou `prisma migrate`. O SQL de sessões foi criado
separadamente em `sql/proposta_criar_sessoes.sql` e executado uma única vez no
banco local autorizado, antes do mapeamento Prisma. Não execute novamente se
`public.sessoes` já existir. A criação em outro ambiente exige revisão da equipe
de Banco de Dados e autorização própria; os arquivos de `database/` são preservados.

### Comparação para revisão da equipe de Banco de Dados

| Antes | Depois |
| --- | --- |
| `public.usuarios` | Estrutura preservada. |
| `public.recuperacao_senha` | Estrutura preservada. |
| Sem armazenamento de sessões | Nova tabela `public.sessoes`. |

| Campo de `sessoes` | Finalidade |
| --- | --- |
| `id` | Chave interna `BIGSERIAL`. |
| `usuario_id` | FK para `usuarios`; suas sessões são removidas se o usuário for excluído. |
| `token_hash` | SHA-256 hexadecimal único, com 64 caracteres. |
| `criado_em` | Momento de criação. |
| `ultima_atividade_em` | Controle de inatividade. |
| `expira_em` | Limite absoluto da sessão. |
| `revogada_em` | Marca de invalidação no logout ou substituição. |

O arquivo SQL acrescenta somente a tabela, sequência, PK, unicidade do hash,
FK, CHECKs de formato/datas e índices por usuário e expiração. Não cria extensões,
triggers nem altera colunas das tabelas anteriores. Os CHECKs permanecem ativos
no PostgreSQL, embora o Prisma não os represente integralmente. O campo inverso
`sessoes` no modelo Prisma `usuarios` é uma relação no cliente, sem coluna nova.
O Backend verifica a validade a cada acesso e não faz limpeza automática dos
registros expirados/revogados; a política de retenção fica para revisão da equipe.

## Escopo e padrão de desenvolvimento

**Implementado:** configuração TypeScript, Express, estrutura em camadas,
scripts de execução e validação, health check, mapeamento do banco, cadastro de
usuários, login, sessões persistentes, logout, hash scrypt, proteção CSRF,
limitação de tentativas, tratamento de erros e testes automatizados.

**Pendente:** recuperação de senha, integração do formulário
frontend com a API e demais funcionalidades de gerenciamento da clínica.

Manter código funcional, simples, legível, escalável, manutenível e sustentável.
Comentários devem explicar apenas regras, decisões ou comportamentos não evidentes;
evitar comentários de linhas óbvias e abstrações antes de existir necessidade.
