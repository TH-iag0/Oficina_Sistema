package br.com.example.autogestao_backend.controller;

import br.com.example.autogestao_backend.dto.OficinaDtos;
import br.com.example.autogestao_backend.service.TecnicoService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tecnico")
@PreAuthorize("hasRole('ADMIN')")
public class TecnicoController {
    private final TecnicoService service;
    public TecnicoController(TecnicoService service) {
        this.service = service;
    }

    @PostMapping("/remaps")
    public OficinaDtos.RemapResposta remap(@Valid @RequestBody OficinaDtos.RemapEntrada dados) {
        return service.cadastrarRemap(dados);
    }

    @GetMapping("/remaps/{id}")
    public OficinaDtos.RemapResposta buscarRemap(@PathVariable UUID id) {
        return service.remap(id);
    }

    @PutMapping("/remaps/{id}")
    public OficinaDtos.RemapResposta atualizarRemap(@PathVariable UUID id, @Valid @RequestBody OficinaDtos.RemapEdicao dados) {
        return service.atualizarRemap(id, dados);
    }

    @PostMapping("/diagnosticos")
    public OficinaDtos.DiagnosticoResposta diagnostico(@Valid @RequestBody OficinaDtos.DiagnosticoEntrada dados) {
        return service.cadastrarDiagnostico(dados);
    }

    @GetMapping("/diagnosticos/{id}")
    public OficinaDtos.DiagnosticoResposta buscarDiagnostico(@PathVariable UUID id) {
        return service.diagnostico(id);
    }

    @PutMapping("/diagnosticos/{id}")
    public OficinaDtos.DiagnosticoResposta atualizarDiagnostico(@PathVariable UUID id, @Valid @RequestBody OficinaDtos.DiagnosticoEdicao dados) {
        return service.atualizarDiagnostico(id, dados);
    }
}
