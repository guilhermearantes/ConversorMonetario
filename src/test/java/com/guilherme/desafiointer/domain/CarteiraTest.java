package com.guilherme.desafiointer.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes da entidade Carteira")
class CarteiraTest {

    @Nested
    @DisplayName("Testes de operações financeiras")
    class OperacoesFinanceiras {

        @Test
        @DisplayName("Deve debitar valor corretamente")
        void deveDebitarValorCorretamente() {
            Carteira carteira = Carteira.builder()
                    .saldo(new BigDecimal("1000.00"))
                    .build();

            carteira.debitar(new BigDecimal("500.00"));

            assertEquals(new BigDecimal("500.00"), carteira.getSaldo());
        }

        @Test
        @DisplayName("Deve creditar valor corretamente")
        void deveCreditarValorCorretamente() {
            Carteira carteira = Carteira.builder()
                    .saldo(new BigDecimal("1000.00"))
                    .build();

            carteira.creditar(new BigDecimal("500.00"));

            assertEquals(new BigDecimal("1500.00"), carteira.getSaldo());
        }
    }
}