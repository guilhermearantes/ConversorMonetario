package com.guilherme.desafiointer.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade que representa uma remessa internacional.
 * Mantém registro de valores, taxas, cotações e dados da transação.
 */
@Entity
@Table(name = "remessas")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Remessa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinatario_id", nullable = false)
    private Usuario destinatario; // Adicionado para possibilitar a inserção de dados do destinatário

    @Column(nullable = false)
    private BigDecimal valor;

    @Column(nullable = false)
    private BigDecimal taxa;

    @Column(name = "moeda_destino", nullable = false)
    private String moedaDestino;

    @Column(nullable = false)
    private BigDecimal cotacao;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @Column(nullable = false)
    private BigDecimal valorConvertido; // Adicionado ao banco e configurado no builder
}