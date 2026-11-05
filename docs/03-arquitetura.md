# Arquitetura

## Visão geral

O Backend oficial está em `backend/`, com Java 21, Spring Boot, Maven, Spring Security, Spring Data JPA e PostgreSQL. O servidor TypeScript/Express e o Prisma não fazem parte da estrutura atual. A compatibilidade de senhas existentes é preservada por um encoder scrypt e testes com Node.js.

```text
Requisição HTTP → filtros Spring Security → Controller → Service
                                                       ↓
                                                   Repository
                                                       ↓
                                                Spring Data JPA
                                                       ↓
                                                  PostgreSQL
```

## Backend

Pacote-base: `backend/src/main/java/br/edu/uninassau/clinicaescola/`.

| Pacote/arquivo | Responsabilidade implementada |
| --- | --- |
| `ClinicaEscolaApplication.java` | Inicialização Spring Boot |
| `controller/` | `UsuarioController` e `AuthController`: contratos HTTP, cookies e respostas públicas |
| `service/` | `ValidadorCadastro`, `UsuarioService` e `AuthService`: validação, cadastro, credenciais e sessões; transações nas operações persistentes |
| `repository/` | `UsuarioRepository` e `SessaoRepository`: acesso JPA e consultas SQL nativas para email e atualização atômica de sessões |
| `entity/` | `Usuario` e `Sessao`: mapeamentos para o schema `public` existente |
| `dto/` | Dados internos validados e envelopes públicos; entidades não são respostas da API |
| `config/` | Beans, segurança HTTP, filtros de origem e rate limiting, cookies e encoder scrypt |
| `exception/` | Exceções de negócio e tradução para erros HTTP sem detalhes internos |

Configurações ficam em `backend/src/main/resources/`; testes em `backend/src/test/java/`; vetores criptográficos em `backend/src/test/resources/`. O Wrapper Maven e `pom.xml` definem a construção. Não há necessidade de representar cada responsabilidade com interfaces adicionais.

## Persistência

`UsuarioService` valida antes de gerar o hash e salvar. A pré-consulta de email não substitui o índice único `uq_usuarios_email`. `saveAndFlush` permite traduzir a violação desse índice em conflito HTTP mesmo sob concorrência.

IDs usam geração por identidade, nascimento usa `LocalDate` e timestamps usam `Instant`. Defaults e trigger do banco administram as datas de usuário. `SessaoRepository` renova atividade atomicamente com `GREATEST`, sem retroceder o horário em requisições fora de ordem; sessões expiradas ou revogadas não são renovadas.

O banco é administrado pela equipe responsável, sem SQL ou migrations automáticas no Spring. Os scripts e seus estados estão descritos na [configuração do ambiente](02-configuracao-ambiente.md#preparação-do-banco).

## Frontend e integração

```text
frontend/
├── hub_inicial/
│   ├── index.html
│   └── style.css
└── login_cadastro/cadastro/
    ├── cadastro.html
    ├── login.html
    └── style.css
```

As páginas são HTML/CSS estáticos. O hub referencia `css/styles.css`, `js/app.js` e páginas ausentes em sua pasta. Textos e contatos demonstrativos precisam de confirmação institucional. Os formulários usam `action="#"` e não têm código de envio JSON, leitura de sessão ou tratamento de respostas. Não há implementação de `localStorage` no Frontend atual, apesar de a documentação antiga descrevê-la.

A API já implementa Cadastro, Login e sessões, mas o fluxo navegador → API → banco ainda depende da integração do Frontend. A API não serve essas páginas e não habilita CORS. A integração deve definir mesma origem via proxy ou configurar CORS restrito. Servir o HTML em outra porta não integra as aplicações. HTTPS e cookies devem ser definidos junto com a origem.

Para contratos e proteções, consultar [Autenticação](04-autenticacao.md). Para pendências, consultar [Status do projeto](05-status-projeto.md).
