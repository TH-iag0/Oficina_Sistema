package br.com.example.autogestao_backend.dto;

import br.com.example.autogestao_backend.model.TipoCliente;
import java.time.Instant;
import java.util.UUID;

public record ClienteResposta(UUID id, TipoCliente tipo, String nome, String documento,
                              String telefone, Instant cadastradoEm, boolean ativo) {}
