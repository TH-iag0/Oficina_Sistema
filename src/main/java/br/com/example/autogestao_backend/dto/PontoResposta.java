package br.com.example.autogestao_backend.dto;

import br.com.example.autogestao_backend.model.TipoMarcacao;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PontoResposta(UUID id, UUID funcionarioId, TipoMarcacao tipo,
                           Instant instante, LocalDate dataRegistro, UUID requisicaoId) {}
