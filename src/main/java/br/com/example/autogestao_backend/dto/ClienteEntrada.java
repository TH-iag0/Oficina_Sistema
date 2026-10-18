package br.com.example.autogestao_backend.dto;

import br.com.example.autogestao_backend.model.TipoCliente;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClienteEntrada(
    @NotNull TipoCliente tipo,
    @NotBlank @Size(max = 160) String nome,
    @Size(max = 20) @Pattern(regexp = "[0-9. /-]*") String documento,
    @Size(max = 30) String telefone
) {}
