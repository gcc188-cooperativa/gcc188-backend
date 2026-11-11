CREATE TABLE usuario (
  id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nome              VARCHAR(120) NOT NULL,
  email             VARCHAR(160) NOT NULL,
  senha_hash        VARCHAR(255) NOT NULL,
  papel             VARCHAR(15)  NOT NULL CHECK (papel IN ('ADMINISTRADOR','FINANCEIRO','MOTORISTA','CLIENTE')),
  ativo             BOOLEAN      NOT NULL DEFAULT TRUE,
  deve_trocar_senha BOOLEAN      NOT NULL DEFAULT FALSE,
  falhas_login      SMALLINT     NOT NULL DEFAULT 0,
  bloqueado_ate     TIMESTAMPTZ,
  versao            BIGINT       NOT NULL DEFAULT 0,
  criado_em         TIMESTAMPTZ  NOT NULL DEFAULT now(),
  atualizado_em     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_usuario_email ON usuario (lower(email));
