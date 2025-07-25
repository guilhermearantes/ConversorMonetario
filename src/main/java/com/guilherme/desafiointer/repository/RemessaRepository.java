package com.guilherme.desafiointer.repository;

import com.guilherme.desafiointer.domain.Remessa;
import com.guilherme.desafiointer.domain.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface RemessaRepository extends JpaRepository<Remessa, Long> {

    @Query("SELECT r FROM Remessa r WHERE r.usuario = :usuario " +
            "AND r.dataCriacao BETWEEN :inicio AND :fim")
    Page<Remessa> buscarHistoricoTransacoes(
            @Param("usuario") Usuario usuario,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            Pageable pageable);

    @Query("SELECT SUM(r.valor) FROM Remessa r WHERE r.usuario = :usuario " +
            "AND r.dataCriacao BETWEEN :inicio AND :fim")
    BigDecimal calcularTotalEnviadoPorPeriodo(
            @Param("usuario") Usuario usuario,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    @Query("SELECT SUM(r.taxa) FROM Remessa r WHERE r.usuario = :usuario " +
            "AND r.dataCriacao BETWEEN :inicio AND :fim")
    BigDecimal calcularTotalTaxasPorPeriodo(
            @Param("usuario") Usuario usuario,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);
}