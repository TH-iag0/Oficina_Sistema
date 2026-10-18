package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.dto.PaginaResposta;
import br.com.example.autogestao_backend.shared.exception.ErroApi;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;

final class Registros {
    private Registros() {}
    static <T> T obter(JpaRepository<T, UUID> repo, UUID id) {
        return repo.findById(id).orElseThrow(() -> new ErroApi(HttpStatus.NOT_FOUND, "Registro não encontrado."));
    }
    static PageRequest pagina(int pagina, int tamanho) {
        return pagina(pagina, tamanho, Sort.by("id"));
    }

    static PageRequest pagina(int pagina, int tamanho, Sort ordem) {
        if (pagina < 0 || tamanho < 1 || tamanho > 100) {
            throw new ErroApi(HttpStatus.BAD_REQUEST, "Paginação inválida.");
        }
        return PageRequest.of(pagina, tamanho, ordem);
    }
    static <T,R> PaginaResposta<R> mapear(Page<T> pagina, Function<T,R> mapper) {
        return new PaginaResposta<>(pagina.getContent().stream().map(mapper).toList(), pagina.getNumber(),
            pagina.getSize(), pagina.getTotalElements(), pagina.getTotalPages());
    }
}
