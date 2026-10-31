# Backend Java — Clínica Escola

Base Java 21 / Spring Boot 4.1.2 para a migração gradual da API Node.js.
O projeto Maven se chama `clinica-escola-backend`; a pasta permanece
`backend-spring-temp/` durante a transição. O backend Node em `../backend/`
continua disponível e é a referência dos contratos e comportamentos existentes.

## Estrutura atual

```text
src/main/java/br/edu/uninassau/clinicaescola/
    ClinicaEscolaApplication.java
src/main/resources/
    application.properties
src/test/java/br/edu/uninassau/clinicaescola/
    ClinicaEscolaApplicationTests.java
```

O pacote-base `br.edu.uninassau.clinicaescola` é preservado. Novos componentes
ficarão em seus subpacotes conforme forem implementados:

- `controller`: entrada e saída HTTP, delegando ao serviço.
- `service`: regras de negócio, validações de domínio e transações necessárias.
- `repository`: persistência com Spring Data JPA, sem regras de negócio.
- `entity`: mapeamentos JPA e integridade das entidades persistidas.
- `dto`: contratos de entrada e saída, com Bean Validation quando apropriado.
- `config` e `exception`: somente configurações e tratamento de erros necessários.

Esses subpacotes e suas classes ainda não foram criados: não há endpoints,
entidades ou funcionalidades migradas nesta etapa. Não criar interfaces,
classes abstratas ou componentes vazios apenas para representar as camadas.

## Configuração e validação

Requer JDK 21. Use o Maven Wrapper fornecido; não é necessário instalar Maven
globalmente. A partir desta pasta, no PowerShell:

```powershell
.\mvnw.cmd -o test-compile
```

Esse comando compila produção e testes sem iniciar a aplicação nem executar
os testes. O modo offline exige Maven, plugins e dependências já disponíveis
localmente; se algo faltar, não haverá download. Em outros sistemas, use `./mvnw`.
O wrapper pode tentar baixar o próprio Maven se ele ainda não estiver em cache;
nesse caso, obtenha autorização antes de executar.

A configuração existente usa PostgreSQL local e as variáveis `DB_USER` e
`DB_PASSWORD`, fornecidas pelo ambiente sem versionar credenciais. Não ler,
copiar ou alterar arquivos `.env` para esta preparação.

Preservar `spring.jpa.hibernate.ddl-auto=none` e `spring.jpa.open-in-view=false`.
A preparação não cria tabelas nem executa SQL ou migrações.

O teste `contextLoads` usa `@SpringBootTest` e inicia o contexto completo,
incluindo a configuração de acesso ao banco. Execute `.\mvnw.cmd -o test`
somente com ambiente e banco autorizados preparados; não é um teste isolado.
A aprovação dessa inicialização não comprova equivalência com a API Node.

## Próxima etapa

Migrar uma funcionalidade por vez, preservando endpoints, códigos e mensagens
de erro, validações, hash de senhas, cookies, sessões e proteções de segurança.
Validar os contratos com testes antes de substituir ou remover o backend Node.
