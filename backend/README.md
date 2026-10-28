# Backend — Clínica Escola

Fundação técnica da API, desenvolvida por Isaac e Aaron. Nesta etapa, o projeto
oferece a estrutura de execução e um health check; as funcionalidades de negócio
serão implementadas posteriormente.

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

Fluxo previsto para funcionalidades com acesso a dados:

```text
routes → controllers → services → repositories → Prisma → PostgreSQL
```

| Local | Responsabilidade |
| --- | --- |
| `src/routes/` | Associar métodos e caminhos HTTP aos controllers. |
| `src/controllers/` | Receber requisições e construir respostas HTTP. |
| `src/services/` | Concentrar regras de negócio; ainda sem implementação. |
| `src/repositories/` | Concentrar acesso a dados; contém a inicialização do Prisma. |
| `src/middlewares/` | Comportamentos compartilhados do ciclo HTTP; ainda sem implementação. |
| `src/app.ts` | Configurar Express, leitura de JSON e rotas. |
| `src/server.ts` | Carregar o ambiente, validar a porta e iniciar o servidor HTTP. |
| `prisma/` | Manter o schema usado pelo Prisma. |

O health check usa apenas rota e controller, pois não possui regra de negócio
nem acesso a dados. Novas camadas devem ser utilizadas conforme a necessidade,
sem abstrações prematuras.

## Validações e teste manual

```sh
npm run prisma:validate
npm run typecheck
npm run build
```

Esses comandos validam o schema, verificam os tipos e compilam o projeto.
`typecheck` também gera o cliente Prisma. Nenhum deles altera tabelas ou exige
conexão com o banco.

Com a API em execução, faça `GET http://localhost:3000/health` pelo navegador ou
por um cliente HTTP, ajustando a porta se necessário. Resultado esperado:

```text
HTTP 200
{"status":"ok"}
```

Esse endpoint verifica a disponibilidade da API, sem testar a conexão com o banco.
Ainda não há suíte de testes automatizados nem script `npm test`.

## Integração futura com o PostgreSQL

O datasource usa o provider `postgresql`. A configuração em `prisma.config.ts`
lê `DATABASE_URL`; `src/repositories/prisma.ts` fornece `getPrisma()`, que cria e
reutiliza um cliente com `PrismaPg` sob demanda. Sem a variável, essa função
lança um erro ao ser chamada.

O schema está intencionalmente sem modelos. Em uma etapa futura, com acesso
autorizado ao banco e `DATABASE_URL` configurada:

```sh
npm run prisma:introspect
npm run prisma:generate
```

A introspecção executa `prisma db pull`: lê o banco existente e atualiza o schema
local, que deve ser revisado antes de utilizar os modelos. A geração seguinte
atualiza o cliente em `src/generated/prisma/`.

Não execute `prisma db push`, `prisma migrate` ou scripts SQL nesta etapa.
O objetivo é mapear o banco existente, preservando suas tabelas e os scripts de
`database/`.

## Escopo e padrão de desenvolvimento

**Implementado:** configuração TypeScript, Express, estrutura em camadas,
scripts de execução e validação, health check e preparação do Prisma.

**Pendente:** configuração e verificação da conexão real, mapeamento das tabelas,
regras de negócio, testes automatizados, usuários, login, cadastro e recuperação
de senha.

Manter código funcional, simples, legível, escalável, manutenível e sustentável.
Comentários devem explicar apenas regras, decisões ou comportamentos não evidentes;
evitar comentários de linhas óbvias e abstrações antes de existir necessidade.
