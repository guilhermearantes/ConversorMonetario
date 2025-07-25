package com.guilherme.desafiointer.repository;

import com.guilherme.desafiointer.domain.TransacaoDiaria;
import com.guilherme.desafiointer.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransacaoDiariaRepository extends JpaRepository<TransacaoDiaria, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TransacaoDiaria> findByUsuarioAndData(Usuario usuario, LocalDate data);

    @Query("SELECT COALESCE(t.valorTotal, 0) FROM TransacaoDiaria t " +
            "WHERE t.usuario = :usuario AND t.data = :data")
    BigDecimal findValorTotalByUsuarioAndData(
            @Param("usuario") Usuario usuario,
            @Param("data") LocalDate data
    );

    List<TransacaoDiaria> findByUsuarioAndDataBetweenOrderByDataDesc(
            Usuario usuario,
            LocalDate inicio,
            LocalDate fim
    );

    @Query("SELECT COUNT(t) > 0 FROM TransacaoDiaria t " +
            "WHERE t.usuario = :usuario AND t.data = :data " +
            "AND t.valorTotal > :limite")
    boolean existsTransacaoExcedendoLimite(
            @Param("usuario") Usuario usuario,
            @Param("data") LocalDate data,
            @Param("limite") BigDecimal limite
    );
}