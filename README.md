# 🏥 Clínica Escola UNINASSAU

Sistema web desenvolvido como projeto acadêmico para o gerenciamento de uma Clínica Escola.

![Status](https://img.shields.io/badge/status-em%20desenvolvimento-yellow?style=for-the-badge)
![HTML](https://img.shields.io/badge/HTML-E34F26?style=for-the-badge&logo=html5&logoColor=white)
![CSS](https://img.shields.io/badge/CSS-1572B6?style=for-the-badge&logo=css3&logoColor=white)
![JavaScript](https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)

## 📋 Sobre o projeto

A Clínica Escola aproxima a formação acadêmica da prática profissional, com atendimento à comunidade. Este sistema reúne:

- um portal com informações sobre a clínica, serviços e orientações;
- telas de **login** e **cadastro** de usuários;
- um banco de dados relacional para armazenar usuários e a recuperação de senha.


## 📁 Estrutura do projeto

```
├── backend/     # API e regras de negócio (a desenvolver)
├── frontend/    # Interface da aplicação
│   ├── index.html       # Página inicial (portal)
│   ├── login.html       # Tela de login
│   ├── cadastro.html    # Tela de cadastro
│   ├── css/styles.css   # Estilos e identidade visual
│   └── js/app.js        # Menu mobile, mostrar/ocultar senha e validações
├── database/    # Scripts SQL
│   └── 001_criar_tabelas.sql
└── docs/        # Documentação do projeto
```

## 🗄️ Banco de dados (PostgreSQL)

O script `database/001_criar_tabelas.sql` cria:

| Tabela / Objeto | Descrição |
|---|---|
| `usuarios` | Nome completo, e-mail, hash da senha, data de nascimento, telefone e datas de criação/atualização |
| `recuperacao_senha` | Tokens (em hash) de recuperação, com data de expiração e de uso, ligados ao usuário |
| `uq_usuarios_email` | Índice único no e-mail, sem diferenciar maiúsculas de minúsculas |
| `atualizar_timestamp()` + trigger | Atualiza `atualizado_em` automaticamente a cada alteração em `usuarios` |

Validações no banco: telefone com 10 ou 11 dígitos numéricos e formato básico de e-mail.

**Como executar:**

```bash
psql -U seu_usuario -d nome_do_banco -f database/001_criar_tabelas.sql
```

## ▶️ Como rodar o frontend

O frontend é estático (HTML, CSS e JavaScript puro). Basta abrir o `index.html` no navegador ou usar uma extensão como o *Live Server* do VS Code.

## ⚠️ Estado atual: protótipo

- O cadastro e o login do frontend **são apenas demonstrativos**: os dados ficam no `localStorage` do navegador e a senha não é armazenada.
- **Não é autenticação segura.** Não use dados pessoais reais.
- O selo com a letra "U" é um elemento visual provisório, não o logotipo oficial.
- A integração real depende do backend, que deve validar as credenciais no servidor e guardar senhas com hash seguro.

## ✅ Progresso

- [x] Estrutura inicial do repositório
- [x] Página inicial (portal) responsiva
- [x] Telas de login e cadastro
- [x] Modelagem inicial do banco de dados (usuários e recuperação de senha)
- [ ] Tela de recuperação de senha (`recuperar-senha.html`)
- [ ] Padronizar o frontend em uma única versão (hoje há duas organizações de arquivos)
- [ ] Definir a tecnologia do backend e criar a API
- [ ] Endpoint de cadastro, com senha criptografada (bcrypt ou argon2)
- [ ] Endpoint de login com autenticação (sessão ou token)
- [ ] Fluxo de recuperação de senha (token com expiração e envio por e-mail)
- [ ] Integrar o frontend com a API e remover o `localStorage` de demonstração
- [ ] Validar telefone e confirmação de senha no frontend, alinhados ao banco
- [ ] Substituir os placeholders pelos dados oficiais da clínica
- [ ] Novas tabelas e telas (ex.: agendamentos, serviços/especialidades)
- [ ] Documentar a API e o diagrama do banco em `docs/`

## 🔜 Próximos passos

1. Criar a tela de recuperação de senha, já que o link existe no login mas a página ainda não.
2. Escolher a stack do backend e subir a primeira rota (cadastro de usuário).
3. Ligar o formulário de cadastro ao banco, guardando só o hash da senha.
4. Implementar o login e proteger as páginas da área do usuário.
5. Evoluir para agendamentos e serviços, conforme o escopo definido pelo grupo.

## 👥 Equipe

Projeto desenvolvido em equipe por alunos do curso de Análise e Desenvolvimento de Sistemas (UNINASSAU).

| Nome | GitHub | Responsabilidade |
|---|---|---|
| Ian e João Henrique | [@IanLucas01](https://github.com/IanLucas01 ) / [@joaohenriiqueee](https://github.com/joaohenriiqueee) | Frontend |
| Leonardo Allan, Josiel e Jhonatas | [@Josielz](https://github.com/Josielz) / [@plandim](https://github.com/plandim) | Banco de dados |
| Isaac e Aaron | [@isaacgbr](https://github.com/isaacgbr)    | Backend |

## 📄 Licença

Projeto acadêmico, sem fins comerciais.
