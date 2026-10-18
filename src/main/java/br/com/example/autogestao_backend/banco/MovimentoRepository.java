package br.com.example.autogestao_backend.banco;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentoRepository extends JpaRepository<MovimentoBancario, UUID> {
    interface Totais {
        java.math.BigDecimal getEntradas();
        java.math.BigDecimal getSaidas();
        long getTotal();
    }

    @org.springframework.data.jpa.repository.Query("""
        select coalesce(sum(case when m.valor > 0 then m.valor else 0 end), 0) as entradas,
            coalesce(sum(case when m.valor < 0 then -m.valor else 0 end), 0) as saidas,
            count(m) as total
        from MovimentoBancario m
        where m.confirmado = true and m.instante >= :inicio and m.instante < :fim
        """)
    Totais totais(Instant inicio, Instant fim);

    Optional<MovimentoBancario> findByContaIdAndOrigemId(String contaId, String origemId);
    List<MovimentoBancario> findByContaIdAndInstanteGreaterThanEqual(String contaId, Instant inicio);
    List<MovimentoBancario> findFirst100ByConfirmadoTrueAndInstanteGreaterThanEqualAndInstanteLessThanOrderByInstanteDesc(
        Instant inicio, Instant fim);
}
