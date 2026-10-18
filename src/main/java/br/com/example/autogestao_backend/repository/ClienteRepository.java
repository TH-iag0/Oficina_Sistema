package br.com.example.autogestao_backend.repository;

import br.com.example.autogestao_backend.model.Cliente;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {
    long countByCadastradoEmGreaterThanEqualAndCadastradoEmLessThan(Instant inicio, Instant fim);
    Optional<Cliente> findByDocumento(String documento);
    boolean existsByDocumento(String documento);
    Page<Cliente> findByAtivoTrueAndNomeContainingIgnoreCase(String nome, Pageable pageable);
    Page<Cliente> findByAtivoTrueAndTelefoneContaining(String telefone, Pageable pageable);
}
