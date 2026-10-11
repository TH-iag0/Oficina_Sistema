package br.com.example.autogestao_backend.model;

import java.util.Objects;
import jakarta.persistence.*;

@Entity
@Table(name = "remap")
public class Remap extends Entidade {
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "item_os_id", nullable = false, updatable = false, unique = true)
    private ItemOrdemServico item;
    @Column(name = "tipo_configurado", nullable = false, updatable = false, length = 80) private String tipoConfigurado;
    @Column(nullable = false, length = 100) private String fabricante;
    @Column(name = "modelo_modulo", nullable = false, length = 120) private String modeloModulo;
    @Column(length = 120) private String hardware;
    @Column(length = 120) private String software;
    @Column(length = 120) private String calibracao;
    @Column(nullable = false, length = 1000) private String objetivo;
    @Column(name = "alteracoes_realizadas", length = 2000) private String alteracoesRealizadas;
    protected Remap() {}
    public Remap(ItemOrdemServico item, String tipoConfigurado, String fabricante, String modeloModulo, String objetivo) {
        this.item = Objects.requireNonNull(item);
        if (item.getCategoria() != CategoriaServico.REMAP_ECU && item.getCategoria() != CategoriaServico.REMAP_TCU)
            throw new IllegalArgumentException("Remap precisa de um serviço ECU ou TCU.");
        this.tipoConfigurado = RegrasModel.texto(tipoConfigurado, 80, "Tipo de remap");
        this.fabricante = RegrasModel.texto(fabricante, 100, "Fabricante");
        this.modeloModulo = RegrasModel.texto(modeloModulo, 120, "Modelo do módulo");
        this.objetivo = RegrasModel.texto(objetivo, 1000, "Objetivo");
    }
    public void identificarVersoes(String hardware, String software, String calibracao) {
        exigirEdicao();
        String h = RegrasModel.opcional(hardware, 120, "Hardware");
        String s = RegrasModel.opcional(software, 120, "Software");
        String c = RegrasModel.opcional(calibracao, 120, "Calibração");
        this.hardware = h; this.software = s; this.calibracao = c;
    }
    public void registrarAlteracoes(String alteracoes) {
        item.exigirEmServico(); exigirEdicao();
        alteracoesRealizadas = RegrasModel.texto(alteracoes, 2000, "Alterações realizadas");
    }
    private void exigirEdicao() {
        StatusOrdemServico status = item.getOrdemServico().getStatus();
        if (item.getConcluidoEm() != null || status == StatusOrdemServico.CANCELADO)
            throw new IllegalStateException("Remap encerrado ou cancelado.");
    }
    public ItemOrdemServico getItem() { return item; }
    public String getTipoConfigurado() { return tipoConfigurado; }
    public String getFabricante() { return fabricante; }
    public String getModeloModulo() { return modeloModulo; }
    public String getHardware() { return hardware; }
    public String getSoftware() { return software; }
    public String getCalibracao() { return calibracao; }
    public String getObjetivo() { return objetivo; }
    public String getAlteracoesRealizadas() { return alteracoesRealizadas; }
}
