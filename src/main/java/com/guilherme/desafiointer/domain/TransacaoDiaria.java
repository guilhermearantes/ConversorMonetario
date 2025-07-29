package com.guilherme.desafiointer.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entidade que representa o controle de transações diárias por usuário.
 * Responsável por rastrear o volume financeiro movimentado por cada usuário em um dia específico.
 */
@Entity
@Table(name = "transacoes_diarias")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransacaoDiaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private BigDecimal valorTotal;

    /**
     * Atualiza o valor total das transações do dia.
     *
     * @param novoTotal novo valor total das transações
     */
    public void atualizarValorTotal(BigDecimal novoTotal) {
        this.valorTotal = novoTotal;
    }
}