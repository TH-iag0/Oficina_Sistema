package br.com.example.autogestao_backend.controller;

import br.com.example.autogestao_backend.dto.OficinaDtos;
import br.com.example.autogestao_backend.dto.PaginaResposta;
import br.com.example.autogestao_backend.service.FinanceiroService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/financeiro")
@PreAuthorize("hasRole('ADMIN')")
public class FinanceiroController {
    private final FinanceiroService service;
    public FinanceiroController(FinanceiroService service) {
        this.service = service;
    }

    @PostMapping("/pagamentos")
    public OficinaDtos.PagamentoResposta receber(@Valid @RequestBody OficinaDtos.PagamentoEntrada dados) {
        return service.receber(dados);
    }

    @GetMapping("/pagamentos")
    public PaginaResposta<OficinaDtos.PagamentoResposta> pagamentos(@RequestParam UUID ordemId, @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanho) { return service.pagamentos(ordemId, pagina, tamanho); }

    @PostMapping("/estornos")
    public OficinaDtos.EstornoResposta estornar(@Valid @RequestBody OficinaDtos.EstornoEntrada dados) {
        return service.estornar(dados);
    }

    @GetMapping("/estornos")
    public PaginaResposta<OficinaDtos.EstornoResposta> estornos(@RequestParam UUID pagamentoId, @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanho) { return service.estornos(pagamentoId, pagina, tamanho); }

    @PostMapping("/despesas")
    public OficinaDtos.DespesaResposta despesa(@Valid @RequestBody OficinaDtos.DespesaEntrada dados) {
        return service.despesa(dados);
    }

    @GetMapping("/despesas")
    public PaginaResposta<OficinaDtos.DespesaResposta> despesas(@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanho) { return service.despesas(pagina, tamanho); }

    @PutMapping("/despesas/{id}/pagamento")
    public OficinaDtos.DespesaResposta pagar(@PathVariable UUID id) {
        return service.pagarDespesa(id);
    }
}
