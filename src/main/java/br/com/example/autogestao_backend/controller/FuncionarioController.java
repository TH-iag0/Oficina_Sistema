package br.com.example.autogestao_backend.controller;

import br.com.example.autogestao_backend.dto.OficinaDtos;
import br.com.example.autogestao_backend.dto.PaginaResposta;
import br.com.example.autogestao_backend.service.FuncionarioService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/funcionarios")
@PreAuthorize("hasRole('ADMIN')")
public class FuncionarioController {
    private final FuncionarioService service;
    public FuncionarioController(FuncionarioService service) {
        this.service = service;
    }

    @PostMapping
    public OficinaDtos.FuncionarioResposta cadastrar(@Valid @RequestBody OficinaDtos.FuncionarioEntrada dados) {
        return service.cadastrar(dados);
    }

    @GetMapping
    public PaginaResposta<OficinaDtos.FuncionarioResposta> listar(@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanho) { return service.listar(pagina, tamanho); }

    @PutMapping("/{id}")
    public OficinaDtos.FuncionarioResposta atualizar(@PathVariable UUID id, @Valid @RequestBody OficinaDtos.FuncionarioEdicao dados) {
        return service.atualizar(id, dados);
    }

    @DeleteMapping("/{id}/exclusao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable UUID id) {
        service.excluir(id);
    }

    @PutMapping("/{id}/pin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void pin(@PathVariable UUID id, @Valid @RequestBody OficinaDtos.PinEntrada dados) {
        service.alterarPin(id, dados);
    }

    @PostMapping("/{id}/jornadas")
    public OficinaDtos.JornadaResposta jornada(@PathVariable UUID id, @Valid @RequestBody OficinaDtos.JornadaEntrada dados) {
        return service.jornada(id, dados);
    }

    @GetMapping("/{id}/jornadas")
    public List<OficinaDtos.JornadaResposta> jornadas(@PathVariable UUID id) {
        return service.jornadas(id);
    }

    @PostMapping("/{id}/jornadas/semana")
    public List<OficinaDtos.JornadaResposta> semana(@PathVariable UUID id, @Valid @RequestBody OficinaDtos.SemanaEntrada dados) {
        return service.configurarSemana(id, dados);
    }

    @PutMapping("/jornadas/{id}/encerramento")
    public OficinaDtos.JornadaResposta encerrar(@PathVariable UUID id, @RequestParam LocalDate fim) {
        return service.encerrarJornada(id, fim);
    }
}
