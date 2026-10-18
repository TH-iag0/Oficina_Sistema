package br.com.example.autogestao_backend.repository;

import br.com.example.autogestao_backend.model.NumeradorOs;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface NumeradorOsRepository extends JpaRepository<NumeradorOs, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from NumeradorOs n where n.id = 1")
    NumeradorOs reservar();
}
