-- PostgreSQL: executado pelo Flyway no schema autogestao.
-- UUIDs permanecem CHAR(36), conforme o mapeamento JPA atual.

CREATE TABLE cliente (
    id CHAR(36) PRIMARY KEY,
    versao BIGINT NOT NULL DEFAULT 0,
    tipo VARCHAR(2) NOT NULL CHECK (tipo IN ('PF', 'PJ')),
    nome VARCHAR(160) NOT NULL CHECK (TRIM(nome) <> ''),
    documento VARCHAR(14),
    telefone VARCHAR(30),
    cadastrado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_cliente_documento UNIQUE (documento),
    CONSTRAINT ck_cliente_documento CHECK (documento IS NULL OR
        (tipo = 'PF' AND LENGTH(documento) = 11) OR (tipo = 'PJ' AND LENGTH(documento) = 14))
);
CREATE INDEX idx_cliente_cadastro ON cliente (cadastrado_em);

CREATE TABLE funcionario (
    id CHAR(36) PRIMARY KEY,
    versao BIGINT NOT NULL DEFAULT 0,
    nome VARCHAR(120) NOT NULL CHECK (TRIM(nome) <> ''),
    matricula VARCHAR(32) NOT NULL CHECK (TRIM(matricula) <> ''),
    pin_hash VARCHAR(100) NOT NULL CHECK (LENGTH(pin_hash) = 60),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_funcionario_matricula UNIQUE (matricula)
);

CREATE TABLE veiculo (
    id CHAR(36) PRIMARY KEY,
    versao BIGINT NOT NULL DEFAULT 0,
    cliente_id CHAR(36) NOT NULL,
    placa VARCHAR(7) NOT NULL CHECK (LENGTH(placa) = 7),
    marca VARCHAR(80) NOT NULL CHECK (TRIM(marca) <> ''),
    modelo VARCHAR(120) NOT NULL CHECK (TRIM(modelo) <> ''),
    quilometragem BIGINT NOT NULL CHECK (quilometragem >= 0),
    proxima_revisao_data DATE,
    proxima_revisao_km BIGINT CHECK (proxima_revisao_km >= 0),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_veiculo_placa UNIQUE (placa),
    CONSTRAINT fk_veiculo_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id)
);
CREATE INDEX idx_veiculo_cliente ON veiculo (cliente_id);
CREATE INDEX idx_veiculo_revisao ON veiculo (proxima_revisao_data);

CREATE TABLE servico (
    id CHAR(36) PRIMARY KEY,
    versao BIGINT NOT NULL DEFAULT 0,
    nome VARCHAR(120) NOT NULL CHECK (TRIM(nome) <> ''),
    categoria VARCHAR(24) NOT NULL CHECK (categoria IN ('REMAP_ECU', 'REMAP_TCU', 'MANUTENCAO', 'DIAGNOSTICO', 'OUTRO')),
    preco_sugerido NUMERIC(15,2) NOT NULL CHECK (preco_sugerido >= 0),
    custo_direto_estimado NUMERIC(15,2) NOT NULL CHECK (custo_direto_estimado >= 0),
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_servico_categoria ON servico (categoria);

CREATE TABLE ordem_servico (
    id CHAR(36) PRIMARY KEY,
    versao BIGINT NOT NULL DEFAULT 0,
    numero BIGINT NOT NULL CHECK (numero > 0),
    cliente_id CHAR(36) NOT NULL,
    veiculo_id CHAR(36) NOT NULL,
    status VARCHAR(24) NOT NULL CHECK (status IN ('RASCUNHO', 'RECEBIDO', 'DIAGNOSTICO', 'AGUARDANDO_APROVACAO',
        'APROVADO', 'EM_SERVICO', 'AGUARDANDO_PECA', 'FINALIZADO', 'ENTREGUE', 'CANCELADO')),
    aberta_em TIMESTAMP WITH TIME ZONE NOT NULL,
    recebida_em TIMESTAMP WITH TIME ZONE,
    aprovada_em TIMESTAMP WITH TIME ZONE,
    consentimento_aprovacao VARCHAR(1000),
    finalizada_em TIMESTAMP WITH TIME ZONE,
    entregue_em TIMESTAMP WITH TIME ZONE,
    previsao_entrega TIMESTAMP WITH TIME ZONE,
    receita_finalizada NUMERIC(15,2) CHECK (receita_finalizada >= 0),
    custo_direto_finalizado NUMERIC(15,2) CHECK (custo_direto_finalizado >= 0),
    CONSTRAINT uk_os_numero UNIQUE (numero),
    CONSTRAINT fk_os_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id),
    CONSTRAINT fk_os_veiculo FOREIGN KEY (veiculo_id) REFERENCES veiculo(id),
    CONSTRAINT ck_os_recebimento CHECK (recebida_em IS NULL OR recebida_em >= aberta_em),
    CONSTRAINT ck_os_aprovacao CHECK (aprovada_em IS NULL OR
        (recebida_em IS NOT NULL AND aprovada_em >= recebida_em AND consentimento_aprovacao IS NOT NULL AND TRIM(consentimento_aprovacao) <> '')),
    CONSTRAINT ck_os_finalizacao CHECK (
        (finalizada_em IS NULL AND receita_finalizada IS NULL AND custo_direto_finalizado IS NULL) OR
        (finalizada_em IS NOT NULL AND aprovada_em IS NOT NULL AND finalizada_em >= aprovada_em
            AND receita_finalizada IS NOT NULL AND custo_direto_finalizado IS NOT NULL)),
    CONSTRAINT ck_os_entrega CHECK (entregue_em IS NULL OR (finalizada_em IS NOT NULL AND entregue_em >= finalizada_em)),
    CONSTRAINT ck_os_status_final CHECK (status NOT IN ('FINALIZADO', 'ENTREGUE') OR finalizada_em IS NOT NULL)
);
CREATE INDEX idx_os_status ON ordem_servico (status);
CREATE INDEX idx_os_finalizada ON ordem_servico (finalizada_em);
CREATE INDEX idx_os_cliente_recebida ON ordem_servico (cliente_id, recebida_em);
CREATE INDEX idx_os_veiculo ON ordem_servico (veiculo_id);

CREATE TABLE historico_status_os (
    id CHAR(36) PRIMARY KEY,
    versao BIGINT NOT NULL DEFAULT 0,
    ordem_servico_id CHAR(36) NOT NULL,
    status VARCHAR(24) NOT NULL CHECK (status IN ('RASCUNHO', 'RECEBIDO', 'DIAGNOSTICO', 'AGUARDANDO_APROVACAO',
        'APROVADO', 'EM_SERVICO', 'AGUARDANDO_PECA', 'FINALIZADO', 'ENTREGUE', 'CANCELADO')),
    instante TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_historico_os FOREIGN KEY (ordem_servico_id) REFERENCES ordem_servico(id)
);
CREATE INDEX idx_historico_os_instante ON historico_status_os (ordem_servico_id, instante);

CREATE TABLE item_ordem_servico (
    id CHAR(36) PRIMARY KEY,
    versao BIGINT NOT NULL DEFAULT 0,
    ordem_servico_id CHAR(36) NOT NULL,
    servico_id CHAR(36) NOT NULL,
    descricao_servico VARCHAR(120) NOT NULL CHECK (TRIM(descricao_servico) <> ''),
    categoria VARCHAR(24) NOT NULL CHECK (categoria IN ('REMAP_ECU', 'REMAP_TCU', 'MANUTENCAO', 'DIAGNOSTICO', 'OUTRO')),
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
    preco_unitario NUMERIC(15,2) NOT NULL CHECK (preco_unitario >= 0),
    custo_direto_unitario NUMERIC(15,2) NOT NULL CHECK (custo_direto_unitario >= 0),
    concluido_em TIMESTAMP WITH TIME ZONE,
    retrabalho BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_retrabalho VARCHAR(500),
    CONSTRAINT fk_item_os FOREIGN KEY (ordem_servico_id) REFERENCES ordem_servico(id),
    CONSTRAINT fk_item_servico FOREIGN KEY (servico_id) REFERENCES servico(id),
    CONSTRAINT ck_item_retrabalho CHECK (NOT retrabalho OR (motivo_retrabalho IS NOT NULL AND TRIM(motivo_retrabalho) <> ''))
);
CREATE INDEX idx_item_conclusao ON item_ordem_servico (concluido_em);
CREATE INDEX idx_item_os ON item_ordem_servico (ordem_servico_id);
CREATE INDEX idx_item_servico ON item_ordem_servico (servico_id);

CREATE TABLE pagamento (
    id CHAR(36) PRIMARY KEY,
    versao BIGINT NOT NULL DEFAULT 0,
    ordem_servico_id CHAR(36) NOT NULL,
    valor NUMERIC(15,2) NOT NULL CHECK (valor > 0),
    forma VARCHAR(24) NOT NULL CHECK (forma IN ('PIX', 'DINHEIRO', 'DEBITO', 'CREDITO', 'TRANSFERENCIA', 'OUTRO')),
    recebido_em TIMESTAMP WITH TIME ZONE NOT NULL,
    requisicao_id CHAR(36) NOT NULL,
    CONSTRAINT uk_pagamento_confirmacao UNIQUE (requisicao_id),
    CONSTRAINT fk_pagamento_os FOREIGN KEY (ordem_servico_id) REFERENCES ordem_servico(id)
);
CREATE INDEX idx_pagamento_recebido ON pagamento (recebido_em);
CREATE INDEX idx_pagamento_os ON pagamento (ordem_servico_id);

CREATE TABLE estorno_pagamento (
    id CHAR(36) PRIMARY KEY,
    versao BIGINT NOT NULL DEFAULT 0,
    pagamento_id CHAR(36) NOT NULL,
    valor NUMERIC(15,2) NOT NULL CHECK (valor > 0),
    motivo VARCHAR(500) NOT NULL CHECK (TRIM(motivo) <> ''),
    instante TIMESTAMP WITH TIME ZONE NOT NULL,
    requisicao_id CHAR(36) NOT NULL,
    CONSTRAINT uk_estorno_confirmacao UNIQUE (requisicao_id),
    CONSTRAINT fk_estorno_pagamento FOREIGN KEY (pagamento_id) REFERENCES pagamento(id)
);
CREATE INDEX idx_estorno_instante ON estorno_pagamento (instante);
CREATE INDEX idx_estorno_pagamento ON estorno_pagamento (pagamento_id);

CREATE TABLE despesa (
    id CHAR(36) PRIMARY KEY,
    versao BIGINT NOT NULL DEFAULT 0,
    descricao VARCHAR(160) NOT NULL CHECK (TRIM(descricao) <> ''),
    categoria VARCHAR(80) NOT NULL CHECK (TRIM(categoria) <> ''),
    valor NUMERIC(15,2) NOT NULL CHECK (valor > 0),
    competencia DATE NOT NULL,
    vencimento DATE NOT NULL,
    paga_em TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_despesa_competencia ON despesa (competencia);
CREATE INDEX idx_despesa_vencimento ON despesa (vencimento);
CREATE INDEX idx_despesa_pagamento ON despesa (paga_em);

CREATE TABLE marcacao_ponto (
    id CHAR(36) PRIMARY KEY,
    funcionario_id CHAR(36) NOT NULL,
    tipo VARCHAR(16) NOT NULL CHECK (tipo IN ('ENTRADA', 'ALMOCO', 'RETORNO', 'SAIDA')),
    instante TIMESTAMP WITH TIME ZONE NOT NULL,
    data_registro DATE NOT NULL,
    requisicao_id CHAR(36) NOT NULL,
    CONSTRAINT uk_ponto_funcionario_requisicao UNIQUE (funcionario_id, requisicao_id),
    CONSTRAINT fk_ponto_funcionario FOREIGN KEY (funcionario_id) REFERENCES funcionario(id)
);
CREATE INDEX idx_ponto_funcionario_instante ON marcacao_ponto (funcionario_id, instante);
CREATE INDEX idx_ponto_data_registro ON marcacao_ponto (data_registro);

CREATE TABLE jornada_trabalho (
    id CHAR(36) PRIMARY KEY,
    versao BIGINT NOT NULL DEFAULT 0,
    funcionario_id CHAR(36) NOT NULL,
    dia_semana VARCHAR(16) NOT NULL CHECK (dia_semana IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    entrada TIME NOT NULL,
    saida TIME NOT NULL,
    intervalo_minutos INTEGER NOT NULL CHECK (intervalo_minutos >= 0),
    tolerancia_minutos INTEGER NOT NULL CHECK (tolerancia_minutos >= 0),
    vigencia_inicio DATE NOT NULL,
    vigencia_fim DATE,
    CONSTRAINT uk_jornada_dia_vigencia UNIQUE (funcionario_id, dia_semana, vigencia_inicio),
    CONSTRAINT fk_jornada_funcionario FOREIGN KEY (funcionario_id) REFERENCES funcionario(id),
    CONSTRAINT ck_jornada_horarios CHECK (entrada <> saida),
    CONSTRAINT ck_jornada_vigencia CHECK (vigencia_fim IS NULL OR vigencia_fim >= vigencia_inicio)
);

CREATE TABLE execucao_servico (
    id CHAR(36) PRIMARY KEY,
    versao BIGINT NOT NULL DEFAULT 0,
    item_os_id CHAR(36) NOT NULL,
    funcionario_id CHAR(36) NOT NULL,
    inicio TIMESTAMP WITH TIME ZONE NOT NULL,
    fim TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_execucao_item FOREIGN KEY (item_os_id) REFERENCES item_ordem_servico(id),
    CONSTRAINT fk_execucao_funcionario FOREIGN KEY (funcionario_id) REFERENCES funcionario(id),
    CONSTRAINT ck_execucao_periodo CHECK (fim IS NULL OR fim >= inicio)
);
CREATE INDEX idx_execucao_funcionario_inicio ON execucao_servico (funcionario_id, inicio);
CREATE INDEX idx_execucao_item ON execucao_servico (item_os_id);

CREATE TABLE remap (
    id CHAR(36) PRIMARY KEY,
    versao BIGINT NOT NULL DEFAULT 0,
    item_os_id CHAR(36) NOT NULL,
    tipo_configurado VARCHAR(80) NOT NULL CHECK (TRIM(tipo_configurado) <> ''),
    fabricante VARCHAR(100) NOT NULL,
    modelo_modulo VARCHAR(120) NOT NULL,
    hardware VARCHAR(120),
    software VARCHAR(120),
    calibracao VARCHAR(120),
    objetivo VARCHAR(1000) NOT NULL,
    alteracoes_realizadas VARCHAR(2000),
    CONSTRAINT uk_remap_item UNIQUE (item_os_id),
    CONSTRAINT fk_remap_item FOREIGN KEY (item_os_id) REFERENCES item_ordem_servico(id)
);

CREATE TABLE diagnostico (
    id CHAR(36) PRIMARY KEY,
    versao BIGINT NOT NULL DEFAULT 0,
    item_os_id CHAR(36) NOT NULL,
    sintomas VARCHAR(2000) NOT NULL,
    equipamento VARCHAR(120),
    dtcs VARCHAR(1000),
    testes_realizados VARCHAR(4000),
    causa_identificada VARCHAR(500),
    reparo_aprovado_id CHAR(36),
    CONSTRAINT uk_diagnostico_item UNIQUE (item_os_id),
    CONSTRAINT fk_diagnostico_item FOREIGN KEY (item_os_id) REFERENCES item_ordem_servico(id),
    CONSTRAINT fk_diagnostico_reparo FOREIGN KEY (reparo_aprovado_id) REFERENCES item_ordem_servico(id)
);
CREATE INDEX idx_diagnostico_reparo ON diagnostico (reparo_aprovado_id);
