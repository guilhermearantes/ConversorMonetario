package com.guilherme.desafiointer.remessa.repository;

import com.guilherme.desafiointer.remessa.domain.Remessa;
import com.guilherme.desafiointer.remessa.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RemessaRepository extends JpaRepository<Remessa, Long> {
    List<Remessa> findByRemetenteAndDataHoraBetween(
            Usuario remetente,
            LocalDateTime inicio,
            LocalDateTime fim
    );
}