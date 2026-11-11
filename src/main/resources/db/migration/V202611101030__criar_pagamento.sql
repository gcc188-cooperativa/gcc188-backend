CREATE TABLE pagamento (
  id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  contrato_id      BIGINT        NOT NULL REFERENCES contrato (id),
  numero_parcela   SMALLINT      NOT NULL CHECK (numero_parcela >= 1),
  data_vencimento  DATE          NOT NULL,
  valor_bruto      NUMERIC(12,2) NOT NULL CHECK (valor_bruto > 0),
  valor_taxa       NUMERIC(12,2) NOT NULL CHECK (valor_taxa >= 0),
  valor_liquido    NUMERIC(12,2) NOT NULL,
  status           VARCHAR(10)   NOT NULL CHECK (status IN
                     ('PREVISTO','PAGO','REPASSADO','CANCELADO')),
  data_baixa       DATE,
  baixado_por_id   BIGINT        REFERENCES usuario (id),
  data_repasse     DATE,
  repassado_por_id BIGINT        REFERENCES usuario (id),
  observacao       VARCHAR(300),
  versao           BIGINT        NOT NULL DEFAULT 0,
  criado_em        TIMESTAMPTZ   NOT NULL DEFAULT now(),
  atualizado_em    TIMESTAMPTZ   NOT NULL DEFAULT now(),
  CONSTRAINT uq_pagamento_parcela UNIQUE (contrato_id, numero_parcela),
  CONSTRAINT ck_pagamento_valores CHECK
    (valor_taxa <= valor_bruto AND valor_liquido = valor_bruto - valor_taxa),
  CONSTRAINT ck_pagamento_baixa   CHECK
    (status NOT IN ('PAGO','REPASSADO') OR data_baixa IS NOT NULL),
  CONSTRAINT ck_pagamento_repasse CHECK
    (status <> 'REPASSADO' OR data_repasse IS NOT NULL)
);
