CREATE TABLE despesa (
  id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  origem            VARCHAR(12)   NOT NULL CHECK (origem IN ('COOPERATIVA','COOPERADO')),
  categoria         VARCHAR(15)   NOT NULL CHECK (categoria IN
                      ('COMBUSTIVEL','PEDAGIO','MANUTENCAO','ADMINISTRATIVA','TRIBUTOS','OUTROS')),
  descricao         VARCHAR(300)  NOT NULL,
  valor             NUMERIC(12,2) NOT NULL CHECK (valor > 0),
  data_despesa      DATE          NOT NULL,
  cooperado_id      BIGINT        REFERENCES cooperado (id),
  contrato_id       BIGINT        REFERENCES contrato (id),
  registrado_por_id BIGINT        NOT NULL REFERENCES usuario (id),
  versao            BIGINT        NOT NULL DEFAULT 0,
  criado_em         TIMESTAMPTZ   NOT NULL DEFAULT now(),
  atualizado_em     TIMESTAMPTZ   NOT NULL DEFAULT now(),
  CONSTRAINT ck_despesa_origem CHECK (
    (origem = 'COOPERATIVA' AND cooperado_id IS NULL AND contrato_id IS NULL) OR
    (origem = 'COOPERADO'   AND cooperado_id IS NOT NULL))
);
