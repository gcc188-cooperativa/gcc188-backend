CREATE TABLE contrato (
  id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  cooperando_id     BIGINT        NOT NULL REFERENCES cooperando (id),
  cliente_id        BIGINT        NOT NULL REFERENCES cliente (id),
  descricao         VARCHAR(500)  NOT NULL,
  valor_total       NUMERIC(12,2) NOT NULL CHECK (valor_total > 0),
  modalidade        VARCHAR(10)   NOT NULL CHECK (modalidade IN ('UNICO','MENSAL')),
  num_parcelas      SMALLINT      NOT NULL CHECK (num_parcelas >= 1),
  dia_vencimento    SMALLINT      CHECK (dia_vencimento BETWEEN 1 AND 28),
  data_inicio       DATE          NOT NULL,
  data_fim          DATE,
  taxa_tipo         VARCHAR(10)   NOT NULL CHECK (taxa_tipo IN ('PERCENTUAL','FIXO')),
  taxa_valor        NUMERIC(12,2) NOT NULL CHECK (taxa_valor > 0),
  status            VARCHAR(20)   NOT NULL CHECK (status IN
                      ('RASCUNHO','PENDENTE_APROVACAO','ATIVO','ENCERRADO','CANCELADO')),
  motivo_reprovacao VARCHAR(300),
  criado_por_id     BIGINT        NOT NULL REFERENCES usuario (id),
  aprovado_por_id   BIGINT        REFERENCES usuario (id),
  aprovado_em       TIMESTAMPTZ,
  versao            BIGINT        NOT NULL DEFAULT 0,
  criado_em         TIMESTAMPTZ   NOT NULL DEFAULT now(),
  atualizado_em     TIMESTAMPTZ   NOT NULL DEFAULT now(),
  CONSTRAINT ck_contrato_taxa_pct CHECK (taxa_tipo <> 'PERCENTUAL' OR taxa_valor <= 100),
  CONSTRAINT ck_contrato_datas    CHECK (data_fim IS NULL OR data_fim >= data_inicio),
  CONSTRAINT ck_contrato_parcelas CHECK (
    (modalidade = 'UNICO'  AND num_parcelas = 1) OR
    (modalidade = 'MENSAL' AND num_parcelas >= 2 AND dia_vencimento IS NOT NULL))
);
