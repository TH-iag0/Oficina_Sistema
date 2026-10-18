package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.dto.OficinaDtos;
import br.com.example.autogestao_backend.model.AjustePonto;
import br.com.example.autogestao_backend.model.Funcionario;
import br.com.example.autogestao_backend.model.MarcacaoPonto;
import br.com.example.autogestao_backend.repository.AjustePontoRepository;
import br.com.example.autogestao_backend.repository.FuncionarioRepository;
import br.com.example.autogestao_backend.repository.MarcacaoPontoRepository;
import br.com.example.autogestao_backend.shared.exception.ErroApi;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AjustePontoService {
    private final AjustePontoRepository ajustes;
    private final MarcacaoPontoRepository marcacoes;
    private final FuncionarioRepository funcionarios;
    private final Clock relogio;
    private final ZoneId fuso;
    public AjustePontoService(AjustePontoRepository ajustes, MarcacaoPontoRepository marcacoes,
        FuncionarioRepository funcionarios, Clock relogio, ZoneId fuso) {
        this.ajustes = ajustes;
        this.marcacoes = marcacoes;
        this.funcionarios = funcionarios;
        this.relogio = relogio;
        this.fuso = fuso;
    }

    @Transactional
    public OficinaDtos.AjusteResposta ajustar(OficinaDtos.AjustePontoEntrada dados, String autor) {
        var funcionario = funcionarios.findParaAtualizacao(dados.funcionarioId())
            .orElseThrow(() -> new ErroApi(HttpStatus.NOT_FOUND, "Funcionário não encontrado."));
        if (!funcionario.isAtivo()) {
            throw new ErroApi(HttpStatus.CONFLICT, "Funcionário inativo.");
        }
        var repetida = ajustes.findByRequisicaoId(dados.requisicaoId());
        if (repetida.isPresent()) {
            validarRepeticao(repetida.get(), dados, autor);
            return resposta(repetida.get());
        }
        MarcacaoPonto original = dados.originalId() == null ? null : Registros.obter(marcacoes, dados.originalId());
        if (original != null && (!original.getFuncionarioId().equals(funcionario.getId()) || ajustes.existsByOriginalId(original.getId()))) {
            throw new ErroApi(HttpStatus.CONFLICT, "Marcação de outro funcionário ou já ajustada.");
        }
        MarcacaoPonto nova = substituir(dados, funcionario);
        return resposta(ajustes.saveAndFlush(new AjustePonto(original, nova, dados.motivo(), autor, dados.requisicaoId(), relogio)));
    }

    public OficinaDtos.AjusteResposta buscar(UUID id) {
        return resposta(Registros.obter(ajustes, id));
    }

    private MarcacaoPonto substituir(OficinaDtos.AjustePontoEntrada dados, Funcionario funcionario) {
        if (dados.tipo() == null && dados.instante() == null) {
            return null;
        }
        if (dados.tipo() == null || dados.instante() == null || dados.instante().isAfter(relogio.instant())) {
            throw new ErroApi(HttpStatus.BAD_REQUEST, "Informe tipo e horário não futuro.");
        }
        return marcacoes.saveAndFlush(MarcacaoPonto.registrar(funcionario, dados.tipo(), UUID.randomUUID(),
            Clock.fixed(dados.instante(), fuso), fuso));
    }

    private void validarRepeticao(AjustePonto ajuste, OficinaDtos.AjustePontoEntrada dados, String autor) {
        var origem = ajuste.getOriginal();
        var nova = ajuste.getSubstituta();
        var funcionario = origem == null ? nova.getFuncionarioId() : origem.getFuncionarioId();
        boolean igual = funcionario.equals(dados.funcionarioId()) && Objects.equals(origem == null ? null : origem.getId(), dados.originalId())
            && Objects.equals(nova == null ? null : nova.getTipo(), dados.tipo())
            && Objects.equals(nova == null ? null : nova.getInstante(), dados.instante())
            && ajuste.getMotivo().equals(dados.motivo().strip()) && ajuste.getAutor().equals(autor);
        if (!igual) {
            throw new ErroApi(HttpStatus.CONFLICT, "Confirmação já utilizada.");
        }
    }

    private OficinaDtos.AjusteResposta resposta(AjustePonto a) {
        return new OficinaDtos.AjusteResposta(a.getId(), a.getOriginal() == null ? null : a.getOriginal().getId(),
            a.getSubstituta() == null ? null : a.getSubstituta().getId(), a.getMotivo(), a.getAutor(), a.getRegistradoEm());
    }
}
