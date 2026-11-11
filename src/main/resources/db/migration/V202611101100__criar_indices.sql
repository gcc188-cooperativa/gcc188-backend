CREATE INDEX ix_contrato_cooperando       ON contrato (cooperando_id, status);
CREATE INDEX ix_contrato_cliente          ON contrato (cliente_id, status);
CREATE INDEX ix_pagamento_status_venc     ON pagamento (status, data_vencimento);
CREATE INDEX ix_despesa_coop_data         ON despesa (cooperando_id, data_despesa);
CREATE INDEX ix_despesa_origem_cat_data   ON despesa (origem, categoria, data_despesa);
CREATE INDEX ix_refresh_usuario           ON refresh_token (usuario_id);
