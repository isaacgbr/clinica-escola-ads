# Cadastro, autenticação e sessões

## Contratos implementados

| Método e caminho | Acesso e entrada | Sucesso |
| --- | --- | --- |
| `POST /usuarios` ou `/usuarios/` | Público; JSON com os seis campos abaixo | `201 {"usuario":{"id":"42","nome":"Maria"}}` |
| `POST /auth/login` ou `/auth/login/` | Público; JSON somente com `email` e `senha`; proteção de origem | `200` com envelope `usuario` e cookie de sessão |
| `GET /auth/me` | Cookie de sessão válido | `200` com envelope `usuario` |
| `POST /auth/logout` | Sessão válida, JSON válido e proteção de origem; pode usar `{}` | `204`, revoga a sessão apresentada e remove o cookie |
| `GET /health` | Público | `200 {"status":"ok"}` |

O ID público é string. Cadastro não autentica automaticamente, não atribui perfil clínico nem cadastra paciente. Outras rotas permanecem bloqueadas pela configuração de segurança.

## Validações do Cadastro

A entrada deve ser um objeto JSON contendo exclusivamente os campos abaixo, todos obrigatórios e do tipo string. Campos extras, inclusive `perfil` e `ativo`, são rejeitados; números e booleanos não são convertidos para texto.

| Campo | Regra atual |
| --- | --- |
| `nome` | Remove espaços externos; não vazio, até 150 pontos de código Unicode e sem NUL |
| `email` | Remove espaços externos e converte para minúsculas; até 150 pontos de código; formato validado por regex, com domínio pontuado, sem pontos consecutivos na parte local |
| `senha` | De 8 a 128 pontos de código e até 512 bytes UTF-8; preserva espaços, sem exigência adicional de complexidade |
| `confirmar_senha` | Texto não vazio, exatamente igual à senha, incluindo espaços |
| `nascimento` | Data real e estrita `YYYY-MM-DD`, ano a partir de 0001, não futura segundo `America/Sao_Paulo`; sem idade mínima |
| `telefone` | Remove espaços, parênteses, pontos e hífens; exige 10 ou 11 dígitos ASCII com DDD; prefixo `+55` não é aceito |

A regra de espaços reproduz o conjunto de `trim()`/`\s` do JavaScript; `String.trim()` do Java não é equivalente. Não há exigência de CPF, sobrenome ou validação de operadora telefônica.

As mensagens específicas substituem as de obrigatoriedade para o mesmo campo. Erros de campos são agregados com a mensagem geral `Confira os campos informados.`; entrada que não seja objeto e campos adicionais são rejeitados antes dessa etapa. [ValidadorCadastro](../backend/src/main/java/br/edu/uninassau/clinicaescola/service/ValidadorCadastro.java) é a referência exata de regex, precedência e mensagens.

`CadastroValidado` é interno, contém os dados normalizados e `LocalDate`, sem confirmação de senha. Seu `toString()` oculta o conteúdo; não deve ser usado como resposta HTTP.

## Login e proteção de senhas

Login aceita apenas `email` e `senha` como textos. Normaliza espaços externos e caixa do email; preserva a senha. Exige formato de email e limites de senha, mas sua validação de email não é idêntica à do Cadastro. Credenciais inexistentes e senha incorreta retornam a mesma mensagem: `Email ou senha inválidos.`. A derivação também é executada quando não existe usuário.

O [NodeScryptPasswordEncoder](../backend/src/main/java/br/edu/uninassau/clinicaescola/config/NodeScryptPasswordEncoder.java) usa Bouncy Castle com scrypt `N=32768`, `r=8`, `p=3`, chave de 64 bytes e salt aleatório de 16 bytes. Formato persistido:

```text
scrypt$32768$8$3$saltHex$chaveHex
```

O salt hexadecimal de 32 caracteres é passado como texto UTF-8 à derivação, não como bytes hexadecimais decodificados. Surrogates isolados são substituídos por U+FFFD para compatibilidade com os hashes existentes. A comparação usa `MessageDigest.isEqual`. Senha, hash e token não são retornados no JSON.

## Sessões e cookies

O login gera identificador aleatório de 32 bytes, enviado como 64 caracteres hexadecimais somente no cookie. Apenas seu SHA-256 é persistido em `public.sessoes`.

- Validade absoluta de 8 horas e limite de inatividade de 30 minutos; atingir qualquer limite bloqueia a sessão.
- Renovação atômica da atividade; não reativa sessão expirada ou revogada nem retrocede timestamps sob concorrência.
- Novo login revoga somente a sessão apresentada no cookie; outros dispositivos permanecem ativos.
- Logout revoga somente a sessão apresentada. Sessão inválida retorna `401`; o filtro remove o cookie.
- Cookie `HttpOnly`, `SameSite=Lax`, `Path=/`, duração máxima de 8 horas. Em HTTPS: `Secure` e nome `__Host-clinica_sessao`; no HTTP local: `clinica_sessao_local`.

`AUTH_ORIGIN` deve ser uma origem exata sem caminho, credenciais, parâmetros ou fragmento. HTTP é permitido apenas em loopback fora de `NODE_ENV=production`; produção exige HTTPS. O padrão local é `http://localhost:8080`.

## Proteções HTTP e limitação de tentativas

Cadastro exige `application/json` e corpo de até 16 KiB. Não utiliza cookies ou autenticação; permanece público. POSTs de autenticação exigem JSON de até 16 KiB, `Origin` igual a `AUTH_ORIGIN`, `X-Requested-With: XMLHttpRequest` e rejeitam `Sec-Fetch-Site: cross-site`. A proteção desses POSTs é feita pelo filtro próprio, enquanto o CSRF padrão é dispensado nessas rotas.

Não há form login, HTTP Basic ou usuário de desenvolvimento automático. Respostas de Cadastro e autenticação recebem `Cache-Control: no-store`.

| Área | Limite e comportamento |
| --- | --- |
| Cadastro | Cinco tentativas por IP em 15 minutos, incluindo sucessos e falhas; a sexta retorna `429` com `Retry-After` igual à espera restante em segundos. Se o mapa estiver saturado, um novo IP recebe `429` com `Retry-After: 1` |
| Login | Limites de cinco por email/IP e vinte por IP em 15 minutos, com reserva para chamadas simultâneas. O contador geral é incrementado antes da verificação do par, então uma tentativa bloqueada pela cota email/IP pode consumir a cota geral. Um sucesso limpa a cota do par e devolve sua reserva à cota geral. Bloqueio retorna `429`, com `Retry-After: 900` |

Contadores do Cadastro e Login são independentes e locais à instância, com capacidade limitada a 10.000 chaves por mapa; saturação rejeita novos identificadores temporariamente. Reiniciar o processo perde os contadores. O IP é o do transporte, sem confiar em cabeçalhos encaminhados; IPv6 é agrupado em `/56`. IP compartilhado pode bloquear usuários legítimos. Proxy e múltiplas instâncias exigem revisão antes de produção.

No Cadastro, tipo incorreto e corpo excessivo são rejeitados antes do contador; JSON malformado dentro do limite consome cota. No Login, origem, tipo, tamanho e JSON são verificados antes da reserva. Não presumir que todos os erros `400` consomem cota ou que os dois limitadores têm regras idênticas.

## Integridade e erros

O índice `uq_usuarios_email`, em `LOWER(email)`, garante unicidade mesmo sob concorrência. As consultas usam igualdade literal, sem curingas `LIKE`/`ILIKE`. `UsuarioService` traduz apenas SQLState `23505` desse índice como email duplicado; outras falhas de integridade permanecem internas.

```json
{"erro":{"codigo":"DADOS_INVALIDOS","mensagem":"Confira os campos informados.","campos":{"email":"Informe um email válido com até 150 caracteres."}}}
```

| HTTP | Código e situação |
| --- | --- |
| `400` | `DADOS_INVALIDOS`: entrada inválida, JSON inválido, tipo ou tamanho incorreto |
| `401` | `CREDENCIAIS_INVALIDAS` no Login ou `NAO_AUTENTICADO` para sessão ausente/inválida |
| `403` | `ORIGEM_NAO_PERMITIDA` na proteção de autenticação ou `ACESSO_NEGADO` na autorização |
| `409` | `EMAIL_JA_CADASTRADO`, mensagem `Email já cadastrado.` |
| `429` | `MUITAS_TENTATIVAS`, mensagem `Muitas tentativas. Aguarde antes de tentar novamente.` |
| `500` | `ERRO_INTERNO`, mensagem `Não foi possível concluir a operação.` |

`campos` aparece quando aplicável. Respostas e logs não devem publicar mensagens SQL, dados recebidos, senhas, hashes ou detalhes internos. O tratamento atual registra apenas a classe da falha inesperada.

## Testes e limitações

Existem testes de validação, serviços, controller e fluxo HTTP, incluindo tipos, campos extras, Unicode, datas, normalização, mensagens, concorrência simulada, limites, cookies, origem e ausência de dados sensíveis. `NodeScryptPasswordEncoderTest` valida vetores e compatibilidade nos dois sentidos com Node. `UsuarioPersistenciaTest` cobre persistência, índice e cadastro concorrente; `AuthPersistenciaTest` cobre sessões no PostgreSQL. `ClinicaEscolaApplicationTests` carrega o contexto com banco autorizado. Comandos e proteções estão no [guia de ambiente](02-configuracao-ambiente.md#testes).

Existência de testes não comprova execução bem-sucedida em cada ambiente. A integração do Frontend, a aplicação do SQL de sessões e a preparação para produção precisam ser validadas separadamente. Não existem recuperação de senha por API, perfis/permissões clínicas ou módulos clínicos implementados.

A restrição do cadastro público é tarefa pendente da Sprint 2, conforme orientação do projeto. Critérios de autorização, perfis e impacto no contrato ainda precisam ser definidos e validados; não representam comportamento atual.
