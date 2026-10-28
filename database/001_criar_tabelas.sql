CREATE TABLE usuarios (
    id               BIGSERIAL     PRIMARY KEY,
    nome_completo    VARCHAR(150)  NOT NULL,
    email            VARCHAR(150)  NOT NULL,
    senha_hash       VARCHAR(255)  NOT NULL,
    data_nascimento  DATE          NOT NULL,
    telefone         VARCHAR(11)   NOT NULL,  
    criado_em        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    atualizado_em    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_usuarios_telefone CHECK (telefone ~ '^[0-9]{10,11}$'),
    CONSTRAINT ck_usuarios_email    CHECK (email LIKE '%_@_%._%')
);


CREATE UNIQUE INDEX uq_usuarios_email ON usuarios (LOWER(email));

CREATE TABLE recuperacao_senha (
    id          BIGSERIAL    PRIMARY KEY,
    usuario_id  BIGINT       NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expira_em   TIMESTAMPTZ  NOT NULL,
    usado_em    TIMESTAMPTZ,
    criado_em   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_recuperacao_senha_usuario ON recuperacao_senha (usuario_id);

CREATE OR REPLACE FUNCTION atualizar_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.atualizado_em = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_usuarios_atualizado_em
BEFORE UPDATE ON usuarios
FOR EACH ROW
EXECUTE FUNCTION atualizar_timestamp();