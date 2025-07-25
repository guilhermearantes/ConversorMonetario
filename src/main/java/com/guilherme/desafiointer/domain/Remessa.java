package com.guilherme.desafiointer.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
}