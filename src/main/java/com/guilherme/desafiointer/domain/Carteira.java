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
    private BigDecimal saldoBRL;

    @Column(nullable = false)
    private BigDecimal saldoUSD;

    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    public void debitar(BigDecimal valor, String moeda) {
        if ("BRL".equalsIgnoreCase(moeda)) {
            if (this.saldoBRL.compareTo(valor) < 0) {
                throw new SaldoInsuficienteException(
                        String.format("Saldo insuficiente em %s para realizar a operação. Saldo atual: %s, Valor solicitado: %s",
                                moeda, this.saldoBRL, valor)
                );
            }
            this.saldoBRL = this.saldoBRL.subtract(valor);
        } else if ("USD".equalsIgnoreCase(moeda)) {
            if (this.saldoUSD.compareTo(valor) < 0) {
                throw new SaldoInsuficienteException(
                        String.format("Saldo insuficiente em %s para realizar a operação. Saldo atual: %s, Valor solicitado: %s",
                                moeda, this.saldoUSD, valor)
                );
            }
            this.saldoUSD = this.saldoUSD.subtract(valor);
        } else {
            throw new IllegalArgumentException("Moeda não suportada: " + moeda);
        }
    }

    public void creditar(BigDecimal valor, String moeda) {
        if ("BRL".equalsIgnoreCase(moeda)) {
            this.saldoBRL = this.saldoBRL.add(valor);
        } else if ("USD".equalsIgnoreCase(moeda)) {
            this.saldoUSD = this.saldoUSD.add(valor);
        } else {
            throw new IllegalArgumentException("Moeda não suportada: " + moeda);
        }
    }
}