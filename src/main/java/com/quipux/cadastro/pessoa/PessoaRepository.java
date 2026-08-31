package com.quipux.cadastro.pessoa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PessoaRepository extends JpaRepository<Pessoa, Long> {

    Optional<Pessoa> findByDocumento(String documento);

    boolean existsByDocumento(String documento);

    boolean existsByEmailIgnoreCase(String email);
}
