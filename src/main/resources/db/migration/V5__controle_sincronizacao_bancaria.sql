ALTER TABLE sincronizacao_bancaria ADD COLUMN item_id VARCHAR(120);
ALTER TABLE sincronizacao_bancaria ADD COLUMN execucao_id VARCHAR(36);
ALTER TABLE sincronizacao_bancaria ADD COLUMN ocupado_ate TIMESTAMP WITH TIME ZONE;
ALTER TABLE sincronizacao_bancaria ADD COLUMN proxima_tentativa TIMESTAMP WITH TIME ZONE;
ALTER TABLE sincronizacao_bancaria ADD COLUMN falhas INTEGER NOT NULL DEFAULT 0;
ALTER TABLE sincronizacao_bancaria ADD COLUMN aviso VARCHAR(500);
