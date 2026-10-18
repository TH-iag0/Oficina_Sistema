package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.dto.OficinaDtos;
import br.com.example.autogestao_backend.dto.PaginaResposta;
import br.com.example.autogestao_backend.model.Funcionario;
import br.com.example.autogestao_backend.model.JornadaTrabalho;
import br.com.example.autogestao_backend.repository.FuncionarioRepository;
import br.com.example.autogestao_backend.repository.JornadaTrabalhoRepository;
import br.com.example.autogestao_backend.shared.exception.ErroApi;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FuncionarioService {
    private final FuncionarioRepository funcionarios;
    private final JornadaTrabalhoRepository jornadas;
    private final PinAuthenticator pins;
    public FuncionarioService(FuncionarioRepository funcionarios, JornadaTrabalhoRepository jornadas, PinAuthenticator pins) {
        this.funcionarios = funcionarios;
        this.jornadas = jornadas;
        this.pins = pins;
    }

    @Transactional
    public OficinaDtos.FuncionarioResposta cadastrar(OficinaDtos.FuncionarioEntrada dados) {
        if (funcionarios.existsByMatricula(dados.matricula().strip())) {
            throw new ErroApi(HttpStatus.CONFLICT, "Matrícula já cadastrada.");
        }
        return resposta(funcionarios.saveAndFlush(new Funcionario(dados.nome(), dados.matricula(), pins.codificar(dados.pin()))));
    }

    public PaginaResposta<OficinaDtos.FuncionarioResposta> listar(int pagina, int tamanho) {
        return Registros.mapear(funcionarios.findAll(Registros.pagina(pagina, tamanho)), this::resposta);
    }

    @Transactional
    public void excluir(UUID id) {
        // Chaves estrangeiras preservam ponto, jornadas e serviços já vinculados.
        funcionarios.delete(bloquear(id));
        funcionarios.flush();
    }

    @Transactional
    public OficinaDtos.FuncionarioResposta atualizar(UUID id, OficinaDtos.FuncionarioEdicao dados) {
        var funcionario = bloquear(id);
        funcionario.alterarNome(dados.nome());
        if (dados.ativo()) {
            funcionario.ativar();
        } else {
            funcionario.desativar();
        }
        return resposta(funcionario);
    }

    @Transactional
    public void alterarPin(UUID id, OficinaDtos.PinEntrada dados) {
        bloquear(id).alterarPinHash(pins.codificar(dados.pin()));
    }

    @Transactional
    public OficinaDtos.JornadaResposta jornada(UUID id, OficinaDtos.JornadaEntrada dados) {
        return cadastrarJornada(bloquear(id), dados);
    }

    private OficinaDtos.JornadaResposta cadastrarJornada(Funcionario funcionario, OficinaDtos.JornadaEntrada dados) {
        var existentes = jornadas.findByFuncionarioIdAndDiaSemana(funcionario.getId(), dados.dia());
        boolean sobreposta = existentes.stream().anyMatch(j ->
            (dados.vigenciaFim() == null || !j.getVigenciaInicio().isAfter(dados.vigenciaFim()))
            && (j.getVigenciaFim() == null || !dados.vigenciaInicio().isAfter(j.getVigenciaFim())));
        if (sobreposta) {
            throw new ErroApi(HttpStatus.CONFLICT, "Jornada com vigência sobreposta.");
        }
        var horario = new JornadaTrabalho.Horario(dados.entrada(), dados.saida(), dados.intervaloMinutos(),
            dados.toleranciaMinutos(), Boolean.TRUE.equals(dados.intervaloAutomatico()));
        var jornada = new JornadaTrabalho(funcionario, dados.dia(), horario, dados.vigenciaInicio());
        if (dados.vigenciaFim() != null) jornada.encerrarVigencia(dados.vigenciaFim());
        return resposta(jornadas.saveAndFlush(jornada));
    }

    public List<OficinaDtos.JornadaResposta> jornadas(UUID id) {
        Registros.obter(funcionarios, id);
        return jornadas.findByFuncionarioId(id).stream().map(this::resposta).toList();
    }

    @Transactional
    public List<OficinaDtos.JornadaResposta> configurarSemana(UUID id, OficinaDtos.SemanaEntrada dados) {
        var dias = new java.util.HashSet<java.time.DayOfWeek>();
        if (dados.jornadas() == null || dados.jornadas().isEmpty() || dados.jornadas().size() > 7) {
            throw new IllegalArgumentException("Selecione de um a sete dias.");
        }
        for (var entrada : dados.jornadas()) {
            if (entrada == null || entrada.dia() == null || !dias.add(entrada.dia())) {
                throw new IllegalArgumentException("Dia da semana repetido ou inválido.");
            }
        }
        var funcionario = bloquear(id);
        if (Boolean.TRUE.equals(dados.encerrarAnteriores())) {
            for (var entrada : dados.jornadas()) encerrarAnterior(id, entrada);
        }
        return dados.jornadas().stream().map(entrada -> cadastrarJornada(funcionario, entrada)).toList();
    }

    private void encerrarAnterior(UUID id, OficinaDtos.JornadaEntrada entrada) {
        for (var anterior : jornadas.findByFuncionarioIdAndDiaSemana(id, entrada.dia())) {
            if (anterior.getVigenciaFim() != null || !anterior.getVigenciaInicio().isBefore(entrada.vigenciaInicio())) continue;
            anterior.encerrarVigencia(entrada.vigenciaInicio().minusDays(1));
        }
    }

    @Transactional
    public OficinaDtos.JornadaResposta encerrarJornada(UUID id, LocalDate fim) {
        var jornada = Registros.obter(jornadas, id);
        bloquear(jornada.getFuncionario().getId());
        jornada.encerrarVigencia(fim);
        return resposta(jornada);
    }

    private Funcionario bloquear(UUID id) {
        return funcionarios.findParaAtualizacao(id).orElseThrow(() -> new ErroApi(HttpStatus.NOT_FOUND, "Funcionário não encontrado."));
    }

    private OficinaDtos.FuncionarioResposta resposta(Funcionario f) {
        return new OficinaDtos.FuncionarioResposta(f.getId(), f.getNome(), f.getMatricula(), f.isAtivo());
    }

    private OficinaDtos.JornadaResposta resposta(JornadaTrabalho j) {
        return new OficinaDtos.JornadaResposta(j.getId(), j.getFuncionario().getId(), j.getDiaSemana(),
            j.getEntrada(), j.getSaida(), j.getIntervaloMinutos(), j.getToleranciaMinutos(), j.getVigenciaInicio(), j.getVigenciaFim(), j.isIntervaloAutomatico());
    }
}
