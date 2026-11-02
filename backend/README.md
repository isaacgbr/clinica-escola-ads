# Backend Java — Clínica Escola

Backend oficial Java 21 / Spring Boot 4.1.2 da Clínica Escola, na pasta `backend/`.
O projeto Maven se chama `clinica-escola-backend`. Os contratos da API Node.js
foram mantidos como referência histórica no Git; o servidor TypeScript foi retirado.

[Guia de desenvolvimento](docs/GUIA_DESENVOLVIMENTO.md).

## Estrutura atual

```text
src/main/java/br/edu/uninassau/clinicaescola/
    ClinicaEscolaApplication.java
    dto/CadastroValidado.java
    exception/CadastroInvalidoException.java
    service/ValidadorCadastro.java
src/main/resources/
    application.properties
src/test/java/br/edu/uninassau/clinicaescola/
    ClinicaEscolaApplicationTests.java
    service/ValidadorCadastroTest.java
```

O pacote-base `br.edu.uninassau.clinicaescola` é preservado. Novos componentes
ficarão em seus subpacotes conforme forem implementados:

- `controller`: entrada e saída HTTP, delegando ao serviço.
- `service`: regras de negócio, validações de domínio e transações necessárias.
- `repository`: persistência com Spring Data JPA, sem regras de negócio.
- `entity`: mapeamentos JPA e integridade das entidades persistidas.
- `dto`: contratos de entrada e saída, com Bean Validation quando apropriado.
- `config` e `exception`: somente configurações e tratamento de erros necessários.

A validação da Etapa 2B foi preservada. A implementação local acrescenta controller,
service, repository, entidade JPA, segurança HTTP e testes do cadastro de usuários.
O hash compatível com Node é implementado com Bouncy Castle. Não criar interfaces,
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
Continuar validando os contratos de Cadastro, autenticação e sessões antes de novas alterações.

## Validação do Cadastro (Etapa 2B)

`ValidadorCadastro` recebe um objeto já decodificado como `Map`, mantendo os
nomes `nome`, `email`, `senha`, `confirmar_senha`, `nascimento` e `telefone`.
Rejeita campos extras e tipos diferentes de string, sem conversão automática.
A camada HTTP preserva esses tipos antes de chamar o validador.

O resultado `CadastroValidado` contém os valores normalizados e nascimento
como `LocalDate`, sem confirmação de senha. É um dado interno, não uma resposta
HTTP; seu `toString()` oculta o conteúdo. `CadastroInvalidoException` preserva
código, mensagem e campos dos erros Node, sem depender de HTTP.

O relógio é fornecido no construtor; o limite de nascimento usa sempre
`America/Sao_Paulo`. As regras de espaços e Unicode reproduzem o Node,
sem substituir a validação por anotações com semântica diferente.

Para executar somente os testes unitários isolados, sem Spring ou banco:

```powershell
.\mvnw.cmd -o "-Dtest=ValidadorCadastroTest" test
```

O teste `ClinicaEscolaApplicationTests` não é selecionado por esse comando.
Aplicam-se os mesmos requisitos de cache e autorização de downloads descritos acima.

## Cadastro de usuários

Veja [contrato, testes e pendências do módulo](docs/CADASTRO_USUARIOS.md).

## Sprint: cadastro, login e sessoes

O encoder Bouncy Castle implementa o formato scrypt do Node (salt hexadecimal usado como texto UTF-8). Cadastro, POST /auth/login, GET /auth/me, POST /auth/logout e GET /health estao implementados. Nenhum perfil clinico e atribuido.

Para os POSTs de autenticacao, usar JSON, Origin igual a AUTH_ORIGIN (padrao local http://localhost:8080) e X-Requested-With: XMLHttpRequest. A producao exige HTTPS; NODE_ENV=production rejeita origem HTTP. O cookie e HttpOnly, SameSite=Lax, Path=/ e Secure em HTTPS. Somente SHA-256 do identificador e persistido em public.sessoes. O prazo e de 8 horas e a inatividade maxima de 30 minutos; logout revoga e novo login substitui somente a sessao apresentada.

Login: cinco falhas por email/IP e vinte por IP em 15 minutos; sucessos nao consomem cota. Contadores locais e limitados em memoria, separados do cadastro. Proxy e armazenamento compartilhado precisam de revisao antes de producao.

Execute `.\mvnw.cmd -o test`. Testes ignorados por falta de ambiente nao representam integracao aprovada. Para PostgreSQL, fornecer CADASTRO_TEST_DATABASE_URL (JDBC local, banco _test ou _teste), ALLOW_DB_TESTS=1, DB_USER e DB_PASSWORD por processo autorizado; nunca ler .env. O schema deve estar previamente aplicado. Nenhuma tabela sera criada pelo Java.

## Arquivos preservados e dependência de Node

O SQL de sessões foi preservado em [sql/proposta_criar_sessoes.sql](sql/proposta_criar_sessoes.sql).
Ele é uma referência para revisão da equipe de Banco de Dados, não um script de
inicialização: não executar automaticamente nem reaplicar em banco existente.

A aplicação Java não depende do servidor TypeScript. A suíte completa ainda exige
o executável `node` no PATH para o teste de compatibilidade scrypt; manter essa
dependência e os vetores em `src/test/resources/scrypt-node-vectors.json`.

O arquivo `.env` local legado foi mantido intacto durante a troca de pastas.
O Spring usa as variáveis do ambiente de execução; não importa esse arquivo
automaticamente. Não copiar ou versionar suas credenciais.
