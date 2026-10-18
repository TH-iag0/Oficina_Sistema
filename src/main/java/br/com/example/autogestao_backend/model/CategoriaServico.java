package br.com.example.autogestao_backend.model;

public enum CategoriaServico {
    REMAP_ECU("Remap ECU"), REMAP_TCU("Remap TCU"), MANUTENCAO("Manutenção"), DIAGNOSTICO("Diagnóstico"), OUTRO("Outro");

    private final String nome;
    CategoriaServico(String nome) { this.nome = nome; }
    public String getNome() { return nome; }
}
