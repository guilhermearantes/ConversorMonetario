package com.guilherme.desafiointer.repository;

import com.guilherme.desafiointer.domain.CotacaoHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface CotacaoHistoricoRepository extends JpaRepository<CotacaoHistorico, Long> {

    @Query("SELECT c FROM CotacaoHistorico c WHERE c.moeda = :moeda " +
            "AND c.dataHora = (SELECT MAX(ch.dataHora) FROM CotacaoHistorico ch WHERE ch.moeda = :moeda)")
    Optional<CotacaoHistorico> findUltimaCotacao(@Param("moeda") String moeda);

    @Query("SELECT c FROM CotacaoHistorico c " +
            "WHERE c.moeda = :moeda " +
            "AND c.isFimDeSemana = false " +
            "ORDER BY c.dataHora DESC")
    Optional<CotacaoHistorico> findUltimaCotacaoUtil(@Param("moeda") String moeda);
}