package com.guilherme.desafiointer.repository;

import com.guilherme.desafiointer.domain.Carteira;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PessimisticLockException;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import jakarta.persistence.QueryHint;
import java.util.Optional;

/**
 * Repositório para gerenciamento de entidades Carteira.
 * Fornece operações CRUD básicas e consultas personalizadas com controle de concorrência
 * para garantir a integridade das operações financeiras.
 */
@Repository
public interface CarteiraRepository extends JpaRepository<Carteira, Long> {

    /**
     * Busca a carteira associada a um usuário específico.
     * Utilizado para consultas simples sem necessidade de bloqueio.
     *
     * @param usuarioId ID do usuário proprietário da carteira
     * @return Optional contendo a carteira se encontrada
     * @throws IllegalArgumentException se usuarioId for nulo
     */
    Optional<Carteira> findByUsuarioId(@NotNull Long usuarioId);

    /**
     * Busca uma carteira por ID do usuário utilizando bloqueio pessimista para controle de concorrência.
     * Este método é utilizado em operações que requerem consistência transacional, como transferências
     * e atualizações de saldo, evitando condições de corrida.
     *
     * @param usuarioId ID do usuário proprietário da carteira
     * @return Optional contendo a carteira se encontrada
     * @throws PessimisticLockException se não for possível obter o bloqueio em 3 segundos
     * @throws IllegalArgumentException se usuarioId for nulo
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Carteira c WHERE c.usuario.id = :usuarioId")
    @QueryHints({
            @QueryHint(name = "javax.persistence.lock.timeout", value = "3000"),
            @QueryHint(name = "org.hibernate.cacheable", value = "false")
    })
    Optional<Carteira> findByUsuarioIdWithPessimisticLock(@NotNull Long usuarioId);
}