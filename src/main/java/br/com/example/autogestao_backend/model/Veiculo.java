package br.com.example.autogestao_backend.model;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import jakarta.persistence.*;

@Entity
@Table(name = "veiculo", uniqueConstraints = @UniqueConstraint(name = "uk_veiculo_placa", columnNames = "placa"))
public class Veiculo extends Entidade {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;
    @Column(nullable = false, length = 7) private String placa;
    @Column(nullable = false, length = 80) private String marca;
    @Column(nullable = false, length = 120) private String modelo;
    @Column(nullable = false) private long quilometragem;
    @Column(name = "proxima_revisao_data") private LocalDate proximaRevisaoData;
    @Column(name = "proxima_revisao_km") private Long proximaRevisaoKm;
    @Column(nullable = false) private boolean ativo = true;

    protected Veiculo() {}
    public Veiculo(Cliente cliente, String placa, String marca, String modelo, long quilometragem) {
        this.cliente = Objects.requireNonNull(cliente);
        String normalizada = RegrasModel.texto(placa, 8, "Placa").replace("-", "").toUpperCase(Locale.ROOT);
        if (!normalizada.matches("[A-Z]{3}\\d[A-Z0-9]\\d{2}")) throw new IllegalArgumentException("Placa brasileira inválida.");
        this.placa = normalizada;
        this.marca = RegrasModel.texto(marca, 80, "Marca");
        this.modelo = RegrasModel.texto(modelo, 120, "Modelo");
        if (quilometragem < 0) throw new IllegalArgumentException("Quilometragem negativa.");
        this.quilometragem = quilometragem;
    }
    public void atualizarQuilometragem(long km) {
        if (km < quilometragem) throw new IllegalArgumentException("Quilometragem não pode diminuir.");
        quilometragem = km;
    }
    public void programarRevisao(LocalDate data, Long km) {
        if (data == null && km == null) throw new IllegalArgumentException("Informe data ou quilometragem da revisão.");
        if (km != null && km <= quilometragem) throw new IllegalArgumentException("Revisão deve ter quilometragem futura.");
        proximaRevisaoData = data; proximaRevisaoKm = km;
    }
    public void desativar() { ativo = false; }
    public Cliente getCliente() { return cliente; }
    public String getPlaca() { return placa; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public long getQuilometragem() { return quilometragem; }
    public LocalDate getProximaRevisaoData() { return proximaRevisaoData; }
    public Long getProximaRevisaoKm() { return proximaRevisaoKm; }
    public boolean isAtivo() { return ativo; }
}
