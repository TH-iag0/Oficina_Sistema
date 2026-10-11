package br.com.example.autogestao_backend.model;

import java.util.Objects;
import jakarta.persistence.*;

@Entity
@Table(name = "diagnostico")
public class Diagnostico extends Entidade {
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "item_os_id", nullable = false, updatable = false, unique = true)
    private ItemOrdemServico item;
    @Column(nullable = false, length = 2000) private String sintomas;
    @Column(length = 120) private String equipamento;
    @Column(length = 1000) private String dtcs;
    @Column(name = "testes_realizados", length = 4000) private String testesRealizados;
    @Column(name = "causa_identificada", length = 500) private String causaIdentificada;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reparo_aprovado_id") private ItemOrdemServico reparoAprovado;
    protected Diagnostico() {}
    public Diagnostico(ItemOrdemServico item, String sintomas) {
        this.item = Objects.requireNonNull(item);
        if (item.getCategoria() != CategoriaServico.DIAGNOSTICO) throw new IllegalArgumentException("Item não é diagnóstico.");
        this.sintomas = RegrasModel.texto(sintomas, 2000, "Sintomas");
    }
    public void registrarResultado(String equipamento, String dtcs, String testes, String causa) {
        if (item.getConcluidoEm() != null || item.getOrdemServico().getStatus() == StatusOrdemServico.CANCELADO)
            throw new IllegalStateException("Diagnóstico encerrado ou cancelado.");
        String e = RegrasModel.opcional(equipamento, 120, "Equipamento");
        String d = RegrasModel.opcional(dtcs, 1000, "DTCs");
        String t = RegrasModel.texto(testes, 4000, "Testes");
        String c = RegrasModel.opcional(causa, 500, "Causa");
        this.equipamento = e; this.dtcs = d; this.testesRealizados = t; this.causaIdentificada = c;
    }
    public void vincularReparoAprovado(ItemOrdemServico reparo) {
        Objects.requireNonNull(reparo);
        if (!item.getOrdemServico().getVeiculo().getId().equals(reparo.getOrdemServico().getVeiculo().getId())
            || reparo.getCategoria() == CategoriaServico.DIAGNOSTICO || reparo.getOrdemServico().getAprovadaEm() == null
            || reparo.getOrdemServico().getStatus() == StatusOrdemServico.CANCELADO)
            throw new IllegalArgumentException("Reparo deve ser aprovado e pertencer ao mesmo veículo.");
        if (reparoAprovado != null) throw new IllegalStateException("Reparo já vinculado.");
        reparoAprovado = reparo;
    }
    public ItemOrdemServico getItem() { return item; }
    public String getSintomas() { return sintomas; }
    public String getEquipamento() { return equipamento; }
    public String getDtcs() { return dtcs; }
    public String getTestesRealizados() { return testesRealizados; }
    public String getCausaIdentificada() { return causaIdentificada; }
    public ItemOrdemServico getReparoAprovado() { return reparoAprovado; }
}
