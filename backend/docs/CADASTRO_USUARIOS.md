# Cadastro de usuários

Este módulo registra contas de acesso; não cadastra pacientes, especialidades ou histórico.
Referência funcional: Regras de Negócio — Clínica Escola, aprovado pelo professor e fornecido pelo responsável pelo projeto. Aplicam-se validação antes de persistir, mensagens claras, hash de senha e proteção de dados. O documento não especifica campos próprios de autocadastro de usuários: preserva-se o contrato TypeScript autorizado nesta tarefa. Não são criados perfis nem concedidos acessos clínicos.

## Estado local

Controller, service, repository JPA, entidade, tratamento de erros e proteção HTTP implementados. Validador e seus testes anteriores preservados. O bean PasswordEncoder compatível com scrypt Node foi concluído com Bouncy Castle autorizado. Nenhum encoder inseguro, hash alternativo incompatível ou implementação criptográfica artesanal foi usado como substituto.

## Contrato

`POST /usuarios` (também aceita barra final), público, exige `application/json` e corpo de até 16 KiB. Campos obrigatórios e exclusivamente string: `nome`, `email`, `senha`, `confirmar_senha`, `nascimento`, `telefone`. Campos adicionais são rejeitados, inclusive `perfil`, `ativo` ou campos de paciente.

Nome/email/telefone são normalizados pelo ValidadorCadastro existente. Senhas preservam espaços, pontos de código Unicode e confirmação exata; nascimento usa LocalDate e calendário de São Paulo. Números JSON não são convertidos para texto. Não há exigência adicional de idade, CPF ou sobrenome.

Sucesso: `201 {"usuario":{"id":"42","nome":"Maria"}}`. ID é string. Entidades e CadastroValidado nunca são retornados diretamente.

Erros: `400 DADOS_INVALIDOS`, `409 EMAIL_JA_CADASTRADO`, `429 MUITAS_TENTATIVAS`, `500 ERRO_INTERNO`; envelope `erro` com `codigo`, `mensagem` e `campos` quando aplicável. Mensagens internas, senhas, hashes e detalhes SQL não são publicados. Login, consulta de sessão, logout e health check são implementados; módulos clínicos continuam bloqueados.

## Segurança e persistência

- Cadastro não utiliza autenticação ou cookies. Os POSTs de autenticação usam proteção de origem, cabeçalho customizado e JSON no filtro próprio; demais operações não autorizadas continuam bloqueadas.
- Sem form login, HTTP Basic, logout automático ou usuário/senha de desenvolvimento gerados pelo Spring.
- Limite de cinco requisições JSON de até 16 KiB por IP em 15 minutos, contando sucessos e falhas; sexta retorna 429 e Retry-After. JSON malformado nessa faixa também consome cota. Tipo incorreto e corpo excessivo são rejeitados antes do contador. Essa ordem difere do parser anterior do Express e é documentada para não pressupor identidade de todos os casos inválidos.
- IP obtido do transporte; cabeçalhos de proxy não são confiados. IPv6 é agrupado em /56. Contadores por instância, não compartilhados, reiniciam com o processo; até 10000 chaves ativas, com rejeição temporária de novos IPs ao atingir capacidade. Redes compartilhadas podem atingir a cota.
- Email usa igualdade literal em LOWER(email). Pré-consulta melhora o retorno normal; índice `uq_usuarios_email` continua sendo a garantia de concorrência. Apenas SQLState 23505 desse índice é traduzido como duplicidade. Outros erros permanecem falhas internas.
- Tabela public.usuarios existente: IDENTITY para BIGSERIAL, LocalDate para DATE, Instant para TIMESTAMPTZ. Datas de criação/atualização são administradas pelos defaults/trigger existentes. Transação envolve o cadastro e saveAndFlush identifica conflito antes da resposta.
- `ddl-auto=none`, inicialização SQL desabilitada e open-in-view=false. Nenhum script, tabela, coluna ou migration criado.

## Testes

Maven e dependências existentes são usados offline. A compatibilidade de hash é validada com vetores Node e verificação dos hashes Java pelo Node.

```powershell
.\mvnw.cmd -o "-Dtest=ValidadorCadastroTest,UsuarioServiceTest,UsuarioControllerTest,CadastroFluxoHttpTest" test
```

Esses testes não abrem PostgreSQL; verificam regras, concorrência simulada, normalização, fluxo HTTP, JSON estrito, limites, isolamento de IP, bloqueio das outras rotas e ausência de dados sensíveis em respostas/logs.

UsuarioPersistenciaTest e o teste de contexto só são habilitados com `ALLOW_DB_TESTS=1` e `CADASTRO_TEST_DATABASE_URL` apontando para PostgreSQL local, banco com letras minúsculas/números/sublinhados terminado em `_test` ou `_teste`, sem parâmetros/fragmentos/credenciais na URL. DB_USER e DB_PASSWORD devem ser fornecidos pelo ambiente autorizado; nunca pelo arquivo .env. Um ambiente sem autorização deve executar somente testes isolados.

Os testes de persistência pressupõem o SQL existente já aplicado pela equipe responsável. Não criam schema nem migrations. Escrevem registros exclusivos UUID e removem somente IDs criados pelo próprio teste. Não foram executados nesta tarefa: nenhum banco de teste autorizado foi confirmado.

## Compatibilidade criptográfica

Node usa scrypt N=32768, r=8, p=3, chave de 64 bytes e salt aleatório de 16 bytes representado por 32 caracteres hexadecimais. Esse salt é passado à derivação como texto UTF-8, não como bytes decodificados. O formato persistido é `scrypt$32768$8$3$saltHex$chaveHex`. A conversão de surrogates isolados deve usar U+FFFD como Node. Há vetores de teste gerados com node:crypto para ASCII, espaços, Unicode e surrogate isolado. O encoder é verificado nos dois sentidos por NodeScryptPasswordEncoderTest.
