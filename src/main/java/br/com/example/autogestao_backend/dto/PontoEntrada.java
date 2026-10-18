package br.com.example.autogestao_backend.dto;

import br.com.example.autogestao_backend.model.TipoMarcacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record PontoEntrada(
    @NotBlank @Size(max = 32) String matricula,
    @NotBlank @Pattern(regexp = "\\d{4,12}") String pin,
    @NotNull TipoMarcacao tipo,
    @NotNull UUID requisicaoId
) {
    @Override
    public String toString() {
        return "PontoEntrada[tipo=" + tipo + ", requisicaoId=" + requisicaoId + "]";
    }
}
