package com.guilherme.desafiointer.remessa.repository;

import com.guilherme.desafiointer.remessa.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    boolean existsByDocumento(String documento);
    boolean existsByEmail(String email);
    Optional<Usuario> findByDocumento(String documento);
}