# Guia de desenvolvimento

## Organização e colaboração

- Backend: API, regras de negócio, autenticação e testes Java.
- Frontend: páginas, estilos, acessibilidade, validação de interface e integração HTTP.
- Banco de Dados: modelagem, integridade, revisão dos scripts e preparação dos ambientes.
- Infraestrutura: execução, variáveis de ambiente, HTTPS, proxy e integração dos componentes. A atribuição dessa responsabilidade deve ser combinada pela equipe.

Mudanças em contratos ou estruturas compartilhadas devem ser alinhadas entre as áreas antes da integração. Não alterar arquivos de outra área sem combinar o escopo.

## Código e revisão

Manter responsabilidades claras na [arquitetura existente](03-arquitetura.md). Usar nomes descritivos, funções coesas e soluções simples. Aplicar Clean Code, SOLID, DRY, KISS e YAGNI quando houver benefício concreto; não criar camadas, interfaces ou funções apenas por preferência estética.

Comentários curtos em PT-BR devem explicar regras e decisões não evidentes, sem narrar o código. Preservar os contratos HTTP, as validações e o tratamento uniforme de erros. Validações de Cadastro e Login não são equivalentes e não devem ser unificadas sem análise.

Proteger dados pessoais, senhas e tokens em respostas, logs, testes e revisões. Não versionar credenciais ou arquivos `.env`. Não executar SQL ou migrações sem autorização da equipe responsável.

## Fluxo Git

1. Criar uma branch por área ou tarefa; não trabalhar diretamente em `main` ou `develop`.
2. Fazer commits pequenos e coerentes, com mensagens claras em PT-BR.
3. Abrir Pull Request para `develop`, descrevendo mudança, validação e pendências.
4. Revisar código e documentação; conferir `git status`, `git diff --stat` e `git diff --check`, incluindo arquivos novos e possíveis dados sensíveis.
5. Validar a integração entre Frontend, Backend e Banco antes de promover `develop` para `main`.

Executar os [testes pertinentes](02-configuracao-ambiente.md#testes), informando falhas e testes ignorados. Integrações só podem usar banco exclusivo de teste autorizado. Um teste ignorado não comprova integração aprovada.

## Manutenção da documentação

Toda alteração relevante deve incluir a atualização da documentação afetada no mesmo Pull Request.

Verificar se houve mudança em configuração, API ou arquitetura, se os documentos continuam corretos e se o [status da Sprint](05-status-projeto.md) precisa ser atualizado. Documentação desatualizada deve ser corrigida antes da aprovação do PR.

O README principal é a apresentação pública. Os detalhes técnicos ficam somente em `docs/`, sem cópias paralelas por módulo. Separar comportamento implementado, proposta e planejamento; não registrar aprovação ou resultado de teste sem evidência.
