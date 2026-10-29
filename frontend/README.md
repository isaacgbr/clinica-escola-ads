# Clínica Escola UNINASSAU — protótipo frontend

Projeto estático feito com HTML, CSS e JavaScript puro.

## Arquivos
- `index.html`: hub/página inicial.
- `css/styles.css`: estilos responsivos e identidade visual azul, branco e amarelo.
- `js/app.js`: menu mobile, mostrar/ocultar senha e validação dos formulários.

## Importante: protótipo, não autenticação real
- O cadastro salva nome, e-mail, telefone e nascimento no `localStorage` deste navegador apenas para demonstrar o fluxo.
- A senha não é armazenada e, por isso, o login de demonstração verifica somente se o e-mail foi cadastrado e se uma senha foi preenchida. Não é autenticação segura.
- Não use dados pessoais reais. Antes da entrega final, integrar login e cadastro ao backend da equipe, que deve validar credenciais no servidor e armazenar senhas com hash seguro.
- Textos de serviços, endereço, telefone e e-mail são placeholders. Substitua pelos dados oficiais confirmados pelo grupo/instituição.
- O selo com a letra “U” é um elemento visual provisório, não o logotipo oficial da UNINASSAU.

## Personalização
- Cores principais ficam no início de `css/styles.css`, em `:root`.
- Texto da página inicial e cards ficam em `index.html`.
- Rotinas do formulário ficam em `js/app.js`.
