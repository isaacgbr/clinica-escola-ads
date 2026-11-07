# Primeiro administrador — Sprint 02

Regra proposta, pendente de validação institucional: somente ADMIN cadastra usuários; a API sempre cria INTERNO.

1. Solicitar revisão/aplicação manual de sql/002_adicionar_perfil_usuarios.sql pela equipe de Banco. Não executado pela aplicação. Preserva dados existentes e atribui INTERNO a todas as contas atuais.
2. Conferir o banco de destino. Configurar conexão pelas variáveis DB_USER, DB_PASSWORD e SPRING_DATASOURCE_URL no ambiente, sem colocá-las no comando ou no repositório. Não usar banco de produção sem autorização.
3. Gerar o JAR com o Maven Wrapper (verify). Em terminal interativo, executar explicitamente:

~~~powershell
java -jar target/clinica-escola-backend-0.0.1-SNAPSHOT.jar --provisionar-primeiro-admin
~~~

4. Confirmar a operação e preencher os mesmos campos exigidos pelo cadastro. Senha e confirmação são solicitadas sem eco. Não usar argumentos, pipes ou arquivos com credenciais.

O comando não abre HTTP, reutiliza validação e scrypt e executa uma transação serializável. Recusa email já existente e a existência de qualquer ADMIN; não promove nem sobrescreve contas. Em concorrência ou falha, não repetir sem conferir o resultado com o responsável pelo banco. A inicialização normal nunca provisiona contas.

Após revisão do SQL, validar o fluxo no banco exclusivo de testes antes da integração. O novo código exige a coluna perfil; não publicar antes da aplicação aprovada do SQL. Para POST /usuarios, enviar cookie de sessão ADMIN, Origin igual a AUTH_ORIGIN, X-Requested-With: XMLHttpRequest e JSON. Anônimo recebe 401; INTERNO recebe 403 (com origem válida). GET /auth/me mantém seu contrato público sem expor perfil.
