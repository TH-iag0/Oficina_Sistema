package br.com.example.autogestao_backend.dto;

import java.util.List;

public record PaginaResposta<T>(List<T> itens, int pagina, int tamanho, long total, int totalPaginas) {}
