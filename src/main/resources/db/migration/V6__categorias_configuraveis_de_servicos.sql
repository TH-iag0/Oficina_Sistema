CREATE TABLE categoria_servico (
    id CHAR(36) PRIMARY KEY,
    versao BIGINT NOT NULL DEFAULT 0,
    nome VARCHAR(80) NOT NULL CHECK (TRIM(nome) <> ''),
    nome_normalizado VARCHAR(80) NOT NULL UNIQUE,
    classificacao VARCHAR(24) NOT NULL CHECK (classificacao IN ('REMAP_ECU', 'REMAP_TCU', 'MANUTENCAO', 'DIAGNOSTICO', 'OUTRO')),
    padrao BOOLEAN NOT NULL DEFAULT FALSE
);

INSERT INTO categoria_servico (id, nome, nome_normalizado, classificacao, padrao) VALUES
('00000000-0000-0000-0000-000000000001', 'Manutenção', 'manutenção', 'MANUTENCAO', TRUE),
('00000000-0000-0000-0000-000000000002', 'Diagnóstico', 'diagnóstico', 'DIAGNOSTICO', TRUE),
('00000000-0000-0000-0000-000000000003', 'Remap ECU', 'remap ecu', 'REMAP_ECU', TRUE),
('00000000-0000-0000-0000-000000000004', 'Remap TCU', 'remap tcu', 'REMAP_TCU', TRUE),
('00000000-0000-0000-0000-000000000005', 'Outro', 'outro', 'OUTRO', TRUE);

ALTER TABLE servico ADD COLUMN categoria_catalogo_id CHAR(36) REFERENCES categoria_servico(id);
CREATE INDEX idx_servico_categoria_catalogo ON servico(categoria_catalogo_id);
UPDATE servico SET categoria_catalogo_id = (SELECT c.id FROM categoria_servico c WHERE c.classificacao = servico.categoria AND c.padrao = TRUE);
