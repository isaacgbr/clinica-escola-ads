# Clínica Escola UNINASSAU — protótipo frontend

Projeto estático feito com HTML, CSS e JavaScript puro.

## Arquivos
- `index.html`: hub/página inicial.
- `login.html`: tela de login demonstrativa.
- `cadastro.html`: tela de cadastro demonstrativa.
- `css/styles.css`: estilos responsivos e identidade visual azul, branco e amarelo.
- `js/app.js`: menu mobile, mostrar/ocultar senha e validação dos formulários.

## Como executar no VS Code
1. Extraia o ZIP para uma pasta no computador.
2. Abra o VS Code e vá em **Arquivo > Abrir Pasta**.
3. Selecione a pasta `clinica-escola-uninassau`.
4. Instale a extensão **Live Server** (Ritwick Dey), se ainda não tiver.
5. Clique com o botão direito em `index.html` e selecione **Open with Live Server**.
6. Teste os links **Entrar** e **Criar conta** no menu.

Também é possível abrir `index.html` diretamente no navegador, mas o Live Server facilita o desenvolvimento.

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
