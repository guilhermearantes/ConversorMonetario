package com.guilherme.desafiointer.repository;

import com.guilherme.desafiointer.domain.TransacaoDiaria;
import com.guilherme.desafiointer.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
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

    /**
     * FUNCIONALIDADE FUTURA
     * Busca transações diárias de um usuário num período específico.
     * Retorna lista ordenada por data decrescente.
     */
    List<TransacaoDiaria> findByUsuarioAndDataBetweenOrderByDataDesc(
            Usuario usuario,
            LocalDate inicio,
            LocalDate fim
    );
}