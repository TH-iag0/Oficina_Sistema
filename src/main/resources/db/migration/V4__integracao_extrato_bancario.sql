CREATE TABLE movimento_bancario (
    id CHAR(36) PRIMARY KEY,
    versao BIGINT NOT NULL DEFAULT 0,
    conta_id VARCHAR(120) NOT NULL,
    origem_id VARCHAR(200) NOT NULL,
    descricao VARCHAR(1000) NOT NULL,
    instante TIMESTAMP WITH TIME ZONE NOT NULL,
    valor NUMERIC(19,2) NOT NULL,
    confirmado BOOLEAN NOT NULL,
    CONSTRAINT uk_movimento_origem UNIQUE (conta_id, origem_id)
);
CREATE INDEX idx_movimento_periodo ON movimento_bancario(instante);
CREATE TABLE sincronizacao_bancaria (id INTEGER PRIMARY KEY, sucesso TIMESTAMP WITH TIME ZONE);
INSERT INTO sincronizacao_bancaria(id) VALUES (1);
