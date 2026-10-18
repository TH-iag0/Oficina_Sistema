package br.com.example.autogestao_backend.service;

import br.com.example.autogestao_backend.dto.PaginaResposta;
import br.com.example.autogestao_backend.dto.PontoEntrada;
import br.com.example.autogestao_backend.dto.PontoResposta;
import br.com.example.autogestao_backend.model.Funcionario;
import br.com.example.autogestao_backend.model.MarcacaoPonto;
import br.com.example.autogestao_backend.model.TipoMarcacao;
import br.com.example.autogestao_backend.repository.FuncionarioRepository;
import br.com.example.autogestao_backend.repository.MarcacaoPontoRepository;
import br.com.example.autogestao_backend.shared.exception.ErroApi;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PontoService {
    private final FuncionarioRepository funcionarios;
    private final MarcacaoPontoRepository marcacoes;
    private final PinAuthenticator encoder;
    private final Clock relogio;
    private final ZoneId fuso;

    public PontoService(FuncionarioRepository funcionarios, MarcacaoPontoRepository marcacoes,
                        PinAuthenticator encoder, Clock relogio, ZoneId fuso) {
        this.funcionarios = funcionarios;
        this.marcacoes = marcacoes;
        this.encoder = encoder;
        this.relogio = relogio;
        this.fuso = fuso;
    }

    @Transactional
    public PontoResposta registrar(PontoEntrada entrada) {
        UUID identificado = funcionarios.findIdByMatricula(entrada.matricula().strip())
            .orElseThrow(this::credencialInvalida);
        Funcionario funcionario = funcionarios.findParaAtualizacao(identificado).orElseThrow(this::credencialInvalida);
        if (!funcionario.isAtivo() || !encoder.matches(entrada.pin(), funcionario.pinHashParaAutenticacao())) {
            throw credencialInvalida();
        }
        var existente = marcacoes.findByFuncionario_IdAndRequisicaoId(funcionario.getId(), entrada.requisicaoId());
        if (existente.isPresent()) {
            return repetir(existente.get(), entrada.tipo());
        }
        validarSequencia(funcionario.getId(), entrada.tipo());
        return resposta(marcacoes.saveAndFlush(MarcacaoPonto.registrar(funcionario, entrada.tipo(),
            entrada.requisicaoId(), relogio, fuso)));
    }

    private PontoResposta repetir(MarcacaoPonto existente, TipoMarcacao tipo) {
        if (marcacoes.contarAjustes(existente.getId()) > 0) {
            throw new ErroApi(HttpStatus.CONFLICT, "Marcação ajustada pelo administrador.");
        }
        if (existente.getTipo() != tipo) {
            throw new ErroApi(HttpStatus.CONFLICT, "Identificador já utilizado para outra marcação.");
        }
        return resposta(existente);
    }

    public PaginaResposta<PontoResposta> historico(UUID funcionarioId, LocalDate inicio, LocalDate fim, int pagina, int tamanho) {
        if (!fim.isAfter(inicio) || fim.isAfter(inicio.plusYears(1))) {
            throw new ErroApi(HttpStatus.BAD_REQUEST, "Informe um período de até um ano com fim exclusivo posterior ao início.");
        }
        if (!funcionarios.existsById(funcionarioId)) {
            throw new ErroApi(HttpStatus.NOT_FOUND, "Funcionário não encontrado.");
        }
        var resultado = marcacoes.findNoPeriodo(funcionarioId, inicio.atStartOfDay(fuso).toInstant(),
            fim.atStartOfDay(fuso).toInstant(), Registros.pagina(pagina, tamanho));
        return Registros.mapear(resultado, this::resposta);
    }

    private void validarSequencia(UUID funcionarioId, TipoMarcacao tipo) {
        var ultima = marcacoes.findUltimasVigentes(funcionarioId, PageRequest.of(0, 1)).stream().findFirst();
        if (ultima.isEmpty()) {
            exigir(tipo == TipoMarcacao.ENTRADA);
            return;
        }
        MarcacaoPonto anterior = ultima.get();
        if (relogio.instant().isBefore(anterior.getInstante())) {
            throw new ErroApi(HttpStatus.CONFLICT, "Horário do servidor anterior à última marcação.");
        }
        if (anterior.getTipo() == TipoMarcacao.SAIDA) {
            exigir(tipo == TipoMarcacao.ENTRADA);
            return;
        }
        exigir(switch (anterior.getTipo()) {
            case ENTRADA -> tipo == TipoMarcacao.ALMOCO || tipo == TipoMarcacao.SAIDA;
            case ALMOCO -> tipo == TipoMarcacao.RETORNO;
            case RETORNO -> tipo == TipoMarcacao.SAIDA;
            case SAIDA -> false;
        });
    }

    private void exigir(boolean valido) {
        if (!valido) {
            throw new ErroApi(HttpStatus.CONFLICT, "Marcação fora da sequência ou duplicada.");
        }
    }

    private ErroApi credencialInvalida() {
        return new ErroApi(HttpStatus.UNAUTHORIZED, "Matrícula ou PIN inválido.");
    }

    private PontoResposta resposta(MarcacaoPonto marcacao) {
        return new PontoResposta(marcacao.getId(), marcacao.getFuncionarioId(), marcacao.getTipo(),
            marcacao.getInstante(), marcacao.getDataRegistro(), marcacao.getRequisicaoId());
    }
}
