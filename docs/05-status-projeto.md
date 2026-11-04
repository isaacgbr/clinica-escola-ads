# Status do projeto

## Estado atual

O repositório contém Backend Java 21/Spring Boot em `backend/`, páginas HTML/CSS em `frontend/` e SQL em `database/`. Cadastro, Login, consulta de sessão, Logout e health check estão implementados na API. O Frontend não está integrado; o fluxo completo navegador/API/PostgreSQL ainda precisa de validação conjunta.

Este registro separa evidência no código de planejamento. Não confirma execução de testes, aplicação de SQL ou aprovação do professor. A atribuição das entregas à Sprint 1 deve ser conferida com o planejamento da equipe.

## Sprint 1 — entregas presentes no repositório

- Estrutura do projeto e Backend oficial Spring Boot com Maven Wrapper.
- Cadastro com validação, hash scrypt e tratamento de email duplicado, inclusive sob concorrência.
- Login e sessões persistentes, expiração, Logout, cookies e proteções HTTP.
- Testes automatizados de regras, HTTP, criptografia e persistência condicional.
- Telas estáticas de Cadastro e Login e hub inicial; navegação e recursos do hub ainda incompletos.
- SQL de usuários e recuperação de senha; proposta de sessões preservada separadamente, sem confirmação de aplicação no ambiente.

Essas entregas não equivalem a homologação integrada ou prontidão para produção.

## Sprint 2 — objetivo e pendências

Objetivo: consolidar o acesso ao sistema e integrar as áreas antes da evolução funcional.

- Restringir o cadastro público: tarefa ainda pendente; definir quem pode cadastrar, os perfis envolvidos e os impactos no fluxo com o professor.
- Integrar Cadastro, Login, consulta de sessão e Logout no Frontend; alinhar JSON, erros e cookies.
- Corrigir caminhos e recursos ausentes do hub e complementar validações da interface sem substituir as do servidor.
- Validar com Banco de Dados o schema exigido, o SQL de sessões e a preparação do banco exclusivo de testes.
- Definir origem, proxy, HTTPS e execução integrada; revisar limitações dos contadores por IP e por instância.
- Executar a suíte completa no ambiente autorizado e registrar resultados reais na revisão de integração.
- Confirmar textos, contatos e identidade visual institucionais.

A ordem e o escopo final dessas atividades devem ser alinhados pela equipe; não há aprovação ou responsável novo atribuído por este documento.

## Sprint 3 — planejamento preliminar

Possíveis evoluções: recuperação de senha e módulos clínicos, como agendamentos e especialidades. A tabela de recuperação de senha já existe no SQL, mas não há fluxo HTTP ou envio de email implementado. Os módulos clínicos não estão implementados. O planejamento não indica execução iniciada e depende da definição de escopo com o professor.

## Decisões a validar com o professor e a equipe

- Política de criação de contas, perfis e permissões; restrição do Cadastro na Sprint 2.
- Escopo e critérios de aceite das Sprints 2 e 3; confirmação das entregas da Sprint 1.
- Fluxo de recuperação de senha e escopo dos primeiros módulos clínicos.
- Critérios de homologação e responsabilidade pela infraestrutura.

## Próximos passos

1. Revisar os contratos de [autenticação](04-autenticacao.md) entre as áreas.
2. Confirmar as decisões de negócio e a preparação do [ambiente](02-configuracao-ambiente.md).
3. Implementar as tarefas aprovadas em mudanças pequenas, com testes e documentação no mesmo PR.
4. Validar a integração em `develop` antes de promover para `main`.

Atualizar este registro somente conforme as entregas forem efetivamente concluídas, sem inventar datas, responsáveis ou aprovações.
