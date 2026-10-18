package br.com.example.autogestao_backend.banco;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface SincronizacaoRepository extends JpaRepository<SincronizacaoBancaria, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SincronizacaoBancaria s where s.id = 1")
    Optional<SincronizacaoBancaria> bloquear();
}
