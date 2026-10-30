# Guia de Desenvolvimento do Backend

## 1. Organização do Backend

O Backend segue a arquitetura funcional em camadas:

```text
Routes → Controllers → Services → Repositories → Prisma → PostgreSQL
```

- **Routes:** associam endpoints aos controllers e middlewares.
- **Controllers:** recebem requisições e constroem respostas HTTP.
- **Services:** validam os dados e aplicam regras de negócio.
- **Repositories:** concentram consultas e persistência via Prisma.
- **Prisma e PostgreSQL:** conectam os modelos à estrutura do banco existente.

Middlewares reúnem autenticação, CSRF, limitação de tentativas e erros.
`config/` concentra configurações; `utils/`, funções compartilhadas.
O cadastro em `usuarios` e a autenticação em `auth` servem como referências.

## 2. Padrões de Código

- Preferir nomes descritivos, como `criarUsuario` e `usuarios.service.ts`.
- Manter funções com responsabilidades claras, separando regras de negócio,
  respostas HTTP e acesso ao banco.
- Aplicar Clean Code, KISS e DRY com bom senso: favorecer clareza e simplicidade,
  compartilhando código quando houver repetição real.
- Preservar o padrão funcional; classes e abstrações só ajudam quando resolvem
  uma necessidade concreta.
- Usar comentários curtos em PT-BR para decisões ou comportamentos não evidentes,
  como o tratamento de fuso horário. Evitar narrar cada linha do código.

## 3. Recomendações de Desenvolvimento

- Consultar módulos existentes antes de implementar uma funcionalidade.
- Priorizar soluções simples, legíveis e reutilizáveis, sem antecipar problemas.
- Evitar refatorações desnecessárias e mudanças fora do escopo.
- Manter validações e tratamento de erros consistentes com os módulos atuais.
- Testar o comportamento alterado, incluindo entradas inválidas e falhas relevantes.
- Atualizar a documentação quando contratos ou formas de uso mudarem.

Instalação, configuração e comandos de validação estão no [README](../README.md).

## 4. Cuidados e Colaboração

- Proteger credenciais e dados sensíveis nas respostas, diagnósticos e testes.
- Preservar os mecanismos existentes de segurança e isolamento do banco de testes.
- Respeitar as responsabilidades da equipe e combinar mudanças que afetem outras áreas.
- Revisar o diff antes da integração, verificando escopo, consistência e efeitos
  sobre funcionalidades existentes.
- Manter o mesmo estilo entre módulos e explicar decisões relevantes na revisão.
