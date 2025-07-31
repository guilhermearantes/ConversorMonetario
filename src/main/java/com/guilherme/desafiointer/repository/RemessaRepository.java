package com.guilherme.desafiointer.repository;

import com.guilherme.desafiointer.domain.Remessa;
import com.guilherme.desafiointer.domain.Usuario;
import org.springframework.cache.annotation.Cacheable;
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

    /**
     * Busca o histórico de transações de um usuário num período específico.
     * Usa cache para melhorar o desempenho para consultas frequentes com os mesmos parâmetros.
     *
     * @param usuario usuário alvo da busca
     * @param inicio início do período
     * @param fim fim do período
     * @param pageable configuração de paginação
     * @return Page<Remessa> contendo as remessas do período
     */
    @Cacheable(value = "historicoTransacoes", key = "#usuario.id + '-' + #inicio + '-' + #fim + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    @Query("SELECT r FROM Remessa r WHERE r.usuario = :usuario " +
            "AND r.dataCriacao BETWEEN :inicio AND :fim")
    Page<Remessa> buscarHistoricoTransacoes(
            @Param("usuario") Usuario usuario,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            Pageable pageable);

    /**
     * FUNCIONALIDADE FUTURA
     * Calcula o total de remessas enviadas por um usuário num período.
     * Usa cache para evitar recalcular valores para períodos já consultados.
     *
     * @param usuario usuário alvo do cálculo
     * @param inicio início do período
     * @param fim fim do período
     * @return BigDecimal representando o valor total enviado
     */
    @Cacheable(value = "totaisTransacoes", key = "#usuario.id + '-' + #inicio + '-' + #fim + '-totalEnviado'")
    @Query("SELECT SUM(r.valor) FROM Remessa r WHERE r.usuario = :usuario " +
            "AND r.dataCriacao BETWEEN :inicio AND :fim")
    BigDecimal calcularTotalEnviadoPorPeriodo(
            @Param("usuario") Usuario usuario,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    /**
     * FUNCIONALIDADE FUTURA
     * Calcula o total de taxas cobradas de um usuário num período.
     * Usa cache para evitar recalcular valores já consultados.
     *
     * @param usuario usuário alvo do cálculo
     * @param inicio início do período
     * @param fim fim do período
     * @return BigDecimal representando o valor total de taxas
     */
    @Cacheable(value = "totaisTransacoes", key = "#usuario.id + '-' + #inicio + '-' + #fim + '-totalTaxas'")
    @Query("SELECT SUM(r.taxa) FROM Remessa r WHERE r.usuario = :usuario " +
            "AND r.dataCriacao BETWEEN :inicio AND :fim")
    BigDecimal calcularTotalTaxasPorPeriodo(
            @Param("usuario") Usuario usuario,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);
}