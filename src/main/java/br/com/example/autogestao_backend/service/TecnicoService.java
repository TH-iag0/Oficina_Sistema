package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.dto.OficinaDtos;
import br.com.example.autogestao_backend.model.Diagnostico;
import br.com.example.autogestao_backend.model.Remap;
import br.com.example.autogestao_backend.repository.DiagnosticoRepository;
import br.com.example.autogestao_backend.repository.RemapRepository;
import br.com.example.autogestao_backend.shared.exception.ErroApi;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TecnicoService {
    private final OrdemServicoService ordens;
    private final RemapRepository remaps;
    private final DiagnosticoRepository diagnosticos;
    public TecnicoService(OrdemServicoService ordens, RemapRepository remaps, DiagnosticoRepository diagnosticos) {
        this.ordens = ordens;
        this.remaps = remaps;
        this.diagnosticos = diagnosticos;
    }

    @Transactional
    public OficinaDtos.RemapResposta cadastrarRemap(OficinaDtos.RemapEntrada dados) {
        var item = ordens.itemBloqueado(dados.itemId());
        if (remaps.findByItemId(item.getId()).isPresent()) {
            throw new ErroApi(HttpStatus.CONFLICT, "Item já possui remap.");
        }
        return resposta(remaps.saveAndFlush(new Remap(item, dados.tipo(), dados.fabricante(), dados.modeloModulo(), dados.objetivo())));
    }

    public OficinaDtos.RemapResposta remap(UUID id) {
        return resposta(Registros.obter(remaps, id));
    }

    @Transactional
    public OficinaDtos.RemapResposta atualizarRemap(UUID id, OficinaDtos.RemapEdicao dados) {
        var remap = Registros.obter(remaps, id);
        ordens.itemBloqueado(remap.getItem().getId());
        remap.identificarVersoes(dados.hardware(), dados.software(), dados.calibracao());
        if (dados.alteracoes() != null) remap.registrarAlteracoes(dados.alteracoes());
        return resposta(remap);
    }

    @Transactional
    public OficinaDtos.DiagnosticoResposta cadastrarDiagnostico(OficinaDtos.DiagnosticoEntrada dados) {
        var item = ordens.itemBloqueado(dados.itemId());
        if (diagnosticos.findByItemId(item.getId()).isPresent()) {
            throw new ErroApi(HttpStatus.CONFLICT, "Item já possui diagnóstico.");
        }
        return resposta(diagnosticos.saveAndFlush(new Diagnostico(item, dados.sintomas())));
    }

    public OficinaDtos.DiagnosticoResposta diagnostico(UUID id) {
        return resposta(Registros.obter(diagnosticos, id));
    }

    @Transactional
    public OficinaDtos.DiagnosticoResposta atualizarDiagnostico(UUID id, OficinaDtos.DiagnosticoEdicao dados) {
        var diagnostico = Registros.obter(diagnosticos, id);
        ordens.itemBloqueado(diagnostico.getItem().getId());
        diagnostico.registrarResultado(dados.equipamento(), dados.dtcs(), dados.testes(), dados.causa());
        if (dados.reparoAprovadoId() != null) diagnostico.vincularReparoAprovado(ordens.itemBloqueado(dados.reparoAprovadoId()));
        return resposta(diagnostico);
    }

    private OficinaDtos.RemapResposta resposta(Remap r) {
        return new OficinaDtos.RemapResposta(r.getId(), r.getItem().getId(), r.getTipoConfigurado(), r.getFabricante(),
            r.getModeloModulo(), r.getObjetivo(), r.getHardware(), r.getSoftware(), r.getCalibracao(), r.getAlteracoesRealizadas());
    }

    private OficinaDtos.DiagnosticoResposta resposta(Diagnostico d) {
        return new OficinaDtos.DiagnosticoResposta(d.getId(), d.getItem().getId(), d.getSintomas(), d.getEquipamento(),
            d.getDtcs(), d.getTestesRealizados(), d.getCausaIdentificada(), d.getReparoAprovado() == null ? null : d.getReparoAprovado().getId());
    }
}
