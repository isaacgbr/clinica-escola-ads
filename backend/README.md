# Backend — Clínica Escola

API desenvolvida por Isaac e Aaron, com health check e cadastro de usuários
persistido no PostgreSQL existente.

## Tecnologias

- **Node.js:** ambiente de execução da API.
- **TypeScript:** tipagem estática e compilação para JavaScript.
- **Express:** servidor HTTP, rotas e middlewares.
- **Prisma:** cliente de acesso a dados e mapeamento do banco existente.
- **PostgreSQL:** banco relacional já existente, acessado pelo adaptador `@prisma/adapter-pg`.
- **dotenv:** carregamento das variáveis de um `.env` local.

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
| `src/services/` | Validar o cadastro, aplicar regras de negócio e gerar o hash da senha. |
| `src/repositories/` | Concentrar acesso a dados; contém a inicialização do Prisma. |
| `src/middlewares/` | Padronizar respostas de erro sem expor detalhes internos. |
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

## Validações e testes

```sh
npm run prisma:validate
npm run typecheck
npm run build
npm test
npm run test:integration
```

`prisma:validate` valida o schema. `typecheck` gera o cliente Prisma e verifica
código e testes com `tsconfig.test.json`, sem emitir JavaScript. O build mantém
`tsconfig.json` e não compila os testes para `dist/`. Esses comandos não alteram
tabelas nem exigem conexão com o banco.

`npm test` executa testes unitários e HTTP locais sem acesso ao banco, incluindo
regressões de NUL no nome, mudança de dia/ano em São Paulo e respostas HTTP 400.
`npm run test:integration` exige `DATABASE_URL` e um PostgreSQL de teste com a
estrutura existente. Verifica persistência, hash, resposta 201, email duplicado
e concorrência; cria registros identificados por UUID e os remove ao final.
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

O schema mapeia `usuarios` e `recuperacao_senha`. O cadastro usa os tipos,
limites e valores padrão do banco existente. A unicidade de email depende do
índice SQL em `LOWER(email)`, inclusive para requisições concorrentes; esse
índice de expressão não é representado como `@unique` no modelo Prisma.

Não execute `prisma db push`, `prisma migrate` ou scripts SQL nesta etapa.
O objetivo é mapear o banco existente, preservando suas tabelas e os scripts de
`database/`.

## Escopo e padrão de desenvolvimento

**Implementado:** configuração TypeScript, Express, estrutura em camadas,
scripts de execução e validação, health check, mapeamento do banco, cadastro de
usuários, hash scrypt, tratamento de erros e testes automatizados.

**Pendente:** login/autenticação, recuperação de senha, integração do formulário
frontend com a API e demais funcionalidades de gerenciamento da clínica.

Manter código funcional, simples, legível, escalável, manutenível e sustentável.
Comentários devem explicar apenas regras, decisões ou comportamentos não evidentes;
evitar comentários de linhas óbvias e abstrações antes de existir necessidade.
