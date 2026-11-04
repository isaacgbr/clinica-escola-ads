# Configuração do ambiente

## Pré-requisitos

- Git, JDK 21 e PostgreSQL local com banco e schema preparados pela equipe de Banco de Dados.
- Maven Wrapper incluído em `backend/`: Maven global não é necessário.
- Node.js no PATH para o teste de compatibilidade scrypt. A aplicação Java não usa um servidor Node.js.
- Navegador para as páginas HTML/CSS; um servidor estático já disponível pode ser usado para visualização.

Conferir `java -version` e `node --version`. O Wrapper configura Maven 3.10.0 em `backend/.mvn/wrapper/maven-wrapper.properties`; o projeto usa Spring Boot 4.1.2 em `backend/pom.xml`.

## PostgreSQL e variáveis

A configuração em [application.properties](../backend/src/main/resources/application.properties) aponta para `jdbc:postgresql://localhost:5432/clinica-escola-spring_dev`.

| Variável | Uso |
| --- | --- |
| `DB_USER`, `DB_PASSWORD` | Credenciais fornecidas ao processo por um canal seguro, nunca versionadas |
| `SPRING_DATASOURCE_URL` | Sobrescrita da URL JDBC de desenvolvimento quando necessária |
| `AUTH_ORIGIN` | Origem exata autorizada nos POSTs de autenticação; padrão `http://localhost:8080`, sem caminho ou barra final |
| `NODE_ENV` | A configuração de cookies reconhece `production` e exige HTTPS nesse ambiente; não seleciona um backend Node |
| `ALLOW_DB_TESTS` | `1` habilita os testes que acessam PostgreSQL |
| `CADASTRO_TEST_DATABASE_URL` | URL JDBC exclusiva dos testes de banco |

As variáveis devem estar no ambiente do processo que executa o Maven. O Spring não importa `.env` automaticamente. Não copiar credenciais para comandos versionados, documentação ou logs.

### Preparação do banco

A equipe de Banco deve preparar o banco correto e revisar os scripts antes de qualquer execução:

- [database/001_criar_tabelas.sql](../database/001_criar_tabelas.sql): usuários, recuperação de senha, índice único em `LOWER(email)` e trigger de atualização.
- [Proposta de sessões](../backend/sql/proposta_criar_sessoes.sql): tabela, restrições e índices de sessões. É uma proposta para revisão, não confirmação de aplicação no ambiente. O comentário do SQL menciona outro nome de banco; confirmar o destino com a equipe antes de executá-lo.

Os scripts não são idempotentes e não devem ser reaplicados automaticamente. A aplicação exige `public.usuarios` e `public.sessoes` compatíveis com as entidades. Não cria schema: `ddl-auto=none`, `spring.sql.init.mode=never` e `open-in-view=false` devem permanecer.

## Execução do Backend

No PowerShell, a partir da raiz:

```powershell
cd backend
.\mvnw.cmd -o test-compile
.\mvnw.cmd -o spring-boot:run
```

O primeiro comando compila produção e testes; o segundo inicia a API. Com a configuração padrão, consultar `GET http://localhost:8080/health`: a resposta esperada é `200 {"status":"ok"}`. O health check é simples e não substitui um teste de persistência.

O modo `-o` não baixa dependências Maven: exige cache completo de plugins e bibliotecas. O Wrapper pode baixar o próprio Maven se não estiver disponível; preparar o cache com autorização antes de usar esses comandos em ambiente sem downloads. Em Linux/macOS, usar `sh ./mvnw` no lugar de `.\mvnw.cmd`.

## Execução do Frontend

Abrir diretamente [cadastro.html](../frontend/login_cadastro/cadastro/cadastro.html) ou [login.html](../frontend/login_cadastro/cadastro/login.html) no navegador para visualizar as telas. Também existe o [hub inicial](../frontend/hub_inicial/index.html).

Para visualizar as páginas, também é possível usar um servidor estático local. Consulte [Frontend e integração](03-arquitetura.md#frontend-e-integração) para conhecer as limitações atuais.

## Testes

Todos os comandos abaixo são executados em `backend/`.

```powershell
# Validação de cadastro isolada, sem Spring ou banco
.\mvnw.cmd -o "-Dtest=ValidadorCadastroTest" test

# Regras, criptografia e HTTP com colaboradores simulados, sem PostgreSQL
.\mvnw.cmd -o "-Dtest=ValidadorCadastroTest,UsuarioServiceTest,AuthServiceTest,NodeScryptPasswordEncoderTest,UsuarioControllerTest,CadastroFluxoHttpTest,AuthControllerTest" test

# Suíte completa; integrações são condicionais
.\mvnw.cmd -o test

# Limpeza dos artefatos, compilação, testes e empacotamento
.\mvnw.cmd -o clean verify
```

A suíte criptográfica usa `node` e [vetores scrypt](../backend/src/test/resources/scrypt-node-vectors.json). Consultar os resultados em `backend/target/surefire-reports/`, distinguindo aprovados, falhos e ignorados.

### Integrações com PostgreSQL

`UsuarioPersistenciaTest`, `AuthPersistenciaTest` e `ClinicaEscolaApplicationTests` exigem `ALLOW_DB_TESTS=1`. `BancoTeste` valida a URL antes de conectar:

- Prefixo `jdbc:postgresql://` e host loopback: `localhost`, `127.0.0.1` ou `[::1]`.
- Banco com letras minúsculas, números ou sublinhados, terminado em `_test` ou `_teste`.
- Sem credenciais, parâmetros ou fragmento na URL.
- `DB_USER` e `DB_PASSWORD` fornecidos ao processo autorizado; schema já preparado, sem geração automática.

Com o banco exclusivo `clinica_escola_test` preparado e as credenciais já disponíveis no PowerShell:

```powershell
$env:ALLOW_DB_TESTS = '1'
$env:CADASTRO_TEST_DATABASE_URL = 'jdbc:postgresql://localhost:5432/clinica_escola_test'
.\mvnw.cmd -o clean verify
```

Não é exigido `NODE_ENV=test` pela proteção Java atual. Sem autorização os testes de banco são ignorados; com autorização e URL inválida, a execução falha. Os testes de persistência gravam dados e removem os registros que criam: nunca apontar para desenvolvimento ou produção.

## Problemas comuns

| Sintoma | Verificação |
| --- | --- |
| Compilação incompatível | Confirmar JDK 21 e `JAVA_HOME` do terminal |
| Artefato indisponível offline | Preparar o cache Maven com autorização; não instalar automaticamente |
| Falha ao iniciar JPA | Conferir PostgreSQL, URL, variáveis e schema com a equipe de Banco |
| POST de autenticação retorna 403 | Conferir `Origin`, `AUTH_ORIGIN`, `X-Requested-With` e origem do navegador |
| Resposta 429 | Respeitar `Retry-After`; redes compartilhadas dividem a cota por IP |
| Formulário não cadastra | Consulte [Frontend e integração](03-arquitetura.md#frontend-e-integração) |

Referências externas: [Maven](https://maven.apache.org/guides/index.html), [Spring](https://spring.io/guides) e [PostgreSQL](https://www.postgresql.org/docs/). As versões efetivas são definidas pelos arquivos do projeto.
