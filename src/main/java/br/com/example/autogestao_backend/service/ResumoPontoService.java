package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.dto.OficinaDtos;
import br.com.example.autogestao_backend.model.ExecucaoServico;
import br.com.example.autogestao_backend.model.JornadaTrabalho;
import br.com.example.autogestao_backend.repository.ExecucaoServicoRepository;
import br.com.example.autogestao_backend.repository.FuncionarioRepository;
import br.com.example.autogestao_backend.repository.JornadaTrabalhoRepository;
import br.com.example.autogestao_backend.repository.MarcacaoPontoRepository;
import br.com.example.autogestao_backend.shared.exception.ErroApi;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ResumoPontoService {
    private final FuncionarioRepository funcionarios;
    private final JornadaTrabalhoRepository jornadas;
    private final MarcacaoPontoRepository marcacoes;
    private final ExecucaoServicoRepository execucoes;
    private final ZoneId fuso;
    public ResumoPontoService(FuncionarioRepository funcionarios, JornadaTrabalhoRepository jornadas,
        MarcacaoPontoRepository marcacoes, ExecucaoServicoRepository execucoes, ZoneId fuso) {
        this.funcionarios = funcionarios;
        this.jornadas = jornadas;
        this.marcacoes = marcacoes;
        this.execucoes = execucoes;
        this.fuso = fuso;
    }

    public OficinaDtos.ResumoPonto resumo(UUID id, LocalDate inicio, LocalDate fim) {
        Registros.obter(funcionarios, id);
        if (!fim.isAfter(inicio) || fim.isAfter(inicio.plusYears(1))) {
            throw new ErroApi(HttpStatus.BAD_REQUEST, "Período inválido: máximo de um ano.");
        }
        var jornadasPorDia = jornadasPorDia(jornadas.findSobrepostas(id, inicio, fim), inicio, fim);
        var pontos = marcacoes.findVigentes(id, inicio.minusDays(2).atStartOfDay(fuso).toInstant(), fim.plusDays(2).atStartOfDay(fuso).toInstant());
        var dias = CalculoPonto.calcular(pontos, jornadasPorDia, inicio, fim, fuso);
        return new OficinaDtos.ResumoPonto(id, inicio, fim, dias.stream().mapToLong(OficinaDtos.DiaPonto::minutosTrabalhados).sum(),
            dias.stream().mapToLong(OficinaDtos.DiaPonto::minutosExtras).sum(), dias.stream().mapToLong(OficinaDtos.DiaPonto::minutosAtraso).sum(),
            minutosEmServicos(id, inicio, fim), dias);
    }

    private Map<LocalDate, JornadaTrabalho> jornadasPorDia(List<JornadaTrabalho> vigencias, LocalDate inicio, LocalDate fim) {
        var resultado = new HashMap<LocalDate, JornadaTrabalho>();
        for (LocalDate data = inicio; data.isBefore(fim); data = data.plusDays(1)) {
            for (var jornada : vigencias) {
                if (data.getDayOfWeek().equals(jornada.getDiaSemana()) && !data.isBefore(jornada.getVigenciaInicio())
                    && (jornada.getVigenciaFim() == null || !data.isAfter(jornada.getVigenciaFim()))) {
                    resultado.put(data, jornada);
                    break;
                }
            }
        }
        return resultado;
    }

    private long minutosEmServicos(UUID id, LocalDate inicio, LocalDate fim) {
        var de = inicio.atStartOfDay(fuso).toInstant();
        var ate = fim.atStartOfDay(fuso).toInstant();
        long segundos = 0;
        var pagina = 0;
        Page<ExecucaoServico> lote;
        do {
            lote = execucoes.findSobrepostas(id, de, ate, PageRequest.of(pagina++, 100));
            for (var execucao : lote) {
                if (execucao.getFim() == null) {
                    continue;
                }
                var a = execucao.getInicio().isBefore(de) ? de : execucao.getInicio();
                var b = execucao.getFim().isAfter(ate) ? ate : execucao.getFim();
                segundos += Duration.between(a, b).getSeconds();
            }
        } while (lote.hasNext());
        return segundos / 60;
    }
}
