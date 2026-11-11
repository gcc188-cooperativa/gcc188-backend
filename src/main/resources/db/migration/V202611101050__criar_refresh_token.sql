CREATE TABLE refresh_token (
  id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  usuario_id  BIGINT      NOT NULL REFERENCES usuario (id),
  token_hash  VARCHAR(64) NOT NULL UNIQUE,   -- SHA-256 em hexadecimal; o valor puro nunca é gravado
  familia     UUID        NOT NULL,          -- cadeia de rotação
  expira_em   TIMESTAMPTZ NOT NULL,
  usado_em    TIMESTAMPTZ,
  revogado_em TIMESTAMPTZ,
  criado_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);
