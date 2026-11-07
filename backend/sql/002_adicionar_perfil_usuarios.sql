-- Proposta incremental para revisão da equipe de Banco. Não executada automaticamente.
-- Regra ADMIN/INTERNO pendente de validação institucional.
-- Pré-requisito: public.usuarios da estrutura original. Executar uma única vez, após aprovação.
BEGIN;

ALTER TABLE public.usuarios
    ADD COLUMN perfil VARCHAR(7) NOT NULL DEFAULT 'INTERNO',
    ADD CONSTRAINT ck_usuarios_perfil CHECK (perfil IN ('ADMIN', 'INTERNO'));

-- O DEFAULT também atribui INTERNO às contas existentes. Nenhuma conta é promovida.
COMMIT;
