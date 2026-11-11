CREATE TABLE cooperando (
  id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  usuario_id    BIGINT       NOT NULL UNIQUE REFERENCES usuario (id),
  matricula     VARCHAR(20)  NOT NULL UNIQUE,
  nome          VARCHAR(120) NOT NULL,
  cpf           VARCHAR(11)  NOT NULL UNIQUE CHECK (cpf ~ '^[0-9]{11}$'),
  telefone      VARCHAR(20),
  data_ingresso DATE         NOT NULL,
  ativo         BOOLEAN      NOT NULL DEFAULT TRUE,
  criado_em     TIMESTAMPTZ  NOT NULL DEFAULT now(),
  atualizado_em TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE cliente (
  id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  usuario_id    BIGINT       NOT NULL UNIQUE REFERENCES usuario (id),
  nome          VARCHAR(120) NOT NULL,
  documento     VARCHAR(14)  NOT NULL UNIQUE CHECK (documento ~ '^[0-9]{11}([0-9]{3})?$'),
  telefone      VARCHAR(20),
  email_contato VARCHAR(160),
  ativo         BOOLEAN      NOT NULL DEFAULT TRUE,
  criado_em     TIMESTAMPTZ  NOT NULL DEFAULT now(),
  atualizado_em TIMESTAMPTZ  NOT NULL DEFAULT now()
);
