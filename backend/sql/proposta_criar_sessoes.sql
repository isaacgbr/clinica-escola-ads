-- Proposta para revisao da equipe de Banco de Dados.
-- Executar somente em clinica_escola local, apos autorizacao explicita.
-- O identificador enviado ao navegador nunca e armazenado em texto puro.

BEGIN;

CREATE TABLE public.sessoes (
    id                   BIGSERIAL    PRIMARY KEY,
    usuario_id           BIGINT       NOT NULL,
    token_hash           VARCHAR(64)  NOT NULL,
    criado_em            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    ultima_atividade_em  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expira_em            TIMESTAMPTZ  NOT NULL,
    revogada_em          TIMESTAMPTZ,

    CONSTRAINT fk_sessoes_usuario
        FOREIGN KEY (usuario_id) REFERENCES public.usuarios (id)
        ON DELETE CASCADE,
    CONSTRAINT uq_sessoes_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_sessoes_token_hash
        CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_sessoes_expiracao
        CHECK (expira_em > criado_em),
    CONSTRAINT ck_sessoes_atividade
        CHECK (ultima_atividade_em >= criado_em AND ultima_atividade_em <= expira_em),
    CONSTRAINT ck_sessoes_revogacao
        CHECK (revogada_em IS NULL OR revogada_em >= criado_em)
);

CREATE INDEX idx_sessoes_usuario ON public.sessoes (usuario_id);
CREATE INDEX idx_sessoes_expiracao ON public.sessoes (expira_em);

COMMIT;
