package com.guilherme.desafiointer.domain;

import com.guilherme.desafiointer.exception.domain.SaldoInsuficienteException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;

@Entity
@Table(name = "carteiras")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class Carteira {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal saldo;

    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    public void debitar(BigDecimal valor) {
        if (this.saldo.compareTo(valor) < 0) {
            throw new SaldoInsuficienteException("Saldo insuficiente para realizar a remessa");
        }
        log.debug("Realizando operação de débito");
        this.saldo = this.saldo.subtract(valor);
    }

    public void creditar(BigDecimal valor) {
        log.debug("Realizando operação de crédito");
        this.saldo = this.saldo.add(valor);
    }
}