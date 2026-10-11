package br.com.example.autogestao_backend.repository;

import java.util.*;
import br.com.example.autogestao_backend.model.Cliente;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {
    Optional<Cliente> findByDocumento(String documento);
    boolean existsByDocumento(String documento);
    Page<Cliente> findByAtivoTrueAndNomeContainingIgnoreCase(String nome, Pageable pageable);
    Page<Cliente> findByAtivoTrueAndTelefoneContaining(String telefone, Pageable pageable);
}
