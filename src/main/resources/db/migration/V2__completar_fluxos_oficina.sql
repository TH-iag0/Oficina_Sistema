CREATE TABLE numerador_os (
    id INTEGER PRIMARY KEY,
    valor BIGINT NOT NULL
);
INSERT INTO numerador_os(id, valor) SELECT 1, COALESCE(MAX(numero), 0) FROM ordem_servico;

CREATE TABLE usuario_admin (
    id CHAR(36) PRIMARY KEY,
    versao BIGINT NOT NULL DEFAULT 0,
    login VARCHAR(120) NOT NULL UNIQUE,
    senha_hash VARCHAR(100) NOT NULL
);

CREATE TABLE ajuste_ponto (
    id CHAR(36) PRIMARY KEY,
    versao BIGINT NOT NULL DEFAULT 0,
    original_id CHAR(36) UNIQUE REFERENCES marcacao_ponto(id),
    substituta_id CHAR(36) REFERENCES marcacao_ponto(id),
    motivo VARCHAR(500) NOT NULL CHECK (TRIM(motivo) <> ''),
    autor VARCHAR(120) NOT NULL CHECK (TRIM(autor) <> ''),
    registrado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    requisicao_id CHAR(36) NOT NULL UNIQUE,
    CHECK (original_id IS NOT NULL OR substituta_id IS NOT NULL)
);
CREATE INDEX idx_ajuste_registrado ON ajuste_ponto(registrado_em);
