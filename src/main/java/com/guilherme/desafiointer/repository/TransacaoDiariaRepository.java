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


/**
 * Repositório para controle de transações diárias por usuário.
 * Fornece métodos para rastreamento e validação de limites diários.
 */
@Repository
public interface TransacaoDiariaRepository extends JpaRepository<TransacaoDiaria, Long> {

    /**
     * Busca a transação diária de um usuário numa data específica.
     *
     * @param usuario usuário alvo da busca
     * @param data data da transação
     * @return Optional<TransacaoDiaria> contendo a transação se encontrada
     */
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

    /**
     * Verifica se existe alguma transação que excede o limite para um usuário numa data.
     *
     * @param usuario usuário a ser verificado
     * @param data data da verificação
     * @param limite valor limite a ser verificado
     * @return boolean indicando se existe transação excedendo o limite
     */
    @Query("SELECT COUNT(t) > 0 FROM TransacaoDiaria t " +
            "WHERE t.usuario = :usuario AND t.data = :data " +
            "AND t.valorTotal > :limite")
    boolean existsTransacaoExcedendoLimite(
            @Param("usuario") Usuario usuario,
            @Param("data") LocalDate data,
            @Param("limite") BigDecimal limite
    );
}