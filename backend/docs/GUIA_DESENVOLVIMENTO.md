# Guia de Desenvolvimento do Backend

## 1. Organização do Backend

O Backend Java/Spring Boot segue a arquitetura em camadas:

```text
Controllers → Services → Repositories → Spring Data JPA → PostgreSQL
```

- **Controllers:** definem os endpoints, recebem requisições e constroem respostas HTTP.
- **Services:** validam os dados e aplicam regras de negócio.
- **Repositories:** concentram consultas e persistência via Spring Data JPA.
- **JPA e PostgreSQL:** mapeiam as entidades à estrutura do banco existente, sem geração automática de tabelas.

Filtros Spring Security reúnem autenticação, CSRF e limitação de tentativas; o tratamento de exceções padroniza os erros.
`config/` concentra configurações e filtros; `dto/`, contratos; `entity/`, entidades; `exception/`, erros.
UsuarioController/UsuarioService e AuthController/AuthService servem como referências.

## 2. Padrões de Código

- Preferir nomes descritivos, como `cadastrar` e `UsuarioService`.
- Manter funções com responsabilidades claras, separando regras de negócio,
  respostas HTTP e acesso ao banco.
- Aplicar Clean Code, KISS e DRY com bom senso: favorecer clareza e simplicidade,
  compartilhando código quando houver repetição real.
- Aplicar POO e abstrações somente quando resolvem
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
