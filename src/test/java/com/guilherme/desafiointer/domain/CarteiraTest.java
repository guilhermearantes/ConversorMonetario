package com.guilherme.desafiointer.domain;

import com.guilherme.desafiointer.exception.domain.SaldoInsuficienteException;
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
        @DisplayName("Deve debitar valor corretamente do saldo em BRL")
        void deveDebitarValorCorretamenteDoSaldoBRL() {
            Carteira carteira = Carteira.builder()
                    .saldoBRL(new BigDecimal("1000.00"))
                    .saldoUSD(new BigDecimal("500.00"))
                    .build();

            carteira.debitar(new BigDecimal("300.00"), "BRL");

            assertEquals(new BigDecimal("700.00"), carteira.getSaldoBRL());
            assertEquals(new BigDecimal("500.00"), carteira.getSaldoUSD(), "O saldo em USD não deve ser alterado");
        }

        @Test
        @DisplayName("Deve debitar valor corretamente do saldo em USD")
        void deveDebitarValorCorretamenteDoSaldoUSD() {
            Carteira carteira = Carteira.builder()
                    .saldoBRL(new BigDecimal("1000.00"))
                    .saldoUSD(new BigDecimal("500.00"))
                    .build();

            carteira.debitar(new BigDecimal("200.00"), "USD");

            assertEquals(new BigDecimal("1000.00"), carteira.getSaldoBRL(), "O saldo em BRL não deve ser alterado");
            assertEquals(new BigDecimal("300.00"), carteira.getSaldoUSD());
        }

        @Test
        @DisplayName("Deve lançar exceção ao debitar valor maior que saldo disponível")
        void deveLancarExcecaoAoDebitarValorMaiorQueSaldo() {
            // Given - Carteira com saldos específicos para BRL e USD
            Usuario usuario = Usuario.builder()
                    .id(1L)
                    .nomeCompleto("Teste Usuario")
                    .email("teste@email.com")
                    .tipoUsuario(TipoUsuario.PF)
                    .documento("123.456.789-00")
                    .build();

            Carteira carteira = Carteira.builder()
                    .id(1L)
                    .usuario(usuario)
                    .saldoBRL(new BigDecimal("1000.00"))  // Saldo BRL: R$ 1.000,00
                    .saldoUSD(new BigDecimal("200.00"))   // Saldo USD: $ 200,00
                    .build();

            // When & Then - Tentar debitar valor maior que saldo BRL disponível
            BigDecimal valorExcessivoBRL = new BigDecimal("1500.00"); // Maior que R$ 1.000,00
            SaldoInsuficienteException exceptionBRL = assertThrows(
                    SaldoInsuficienteException.class,
                    () -> carteira.debitar(valorExcessivoBRL, "BRL"),
                    "Deve lançar exceção ao tentar debitar R$ 1.500,00 quando saldo BRL é R$ 1.000,00"
            );

            assertAll("Validação da exceção para saldo BRL insuficiente",
                    () -> assertTrue(exceptionBRL.getMessage().contains("BRL"),
                            "Mensagem deve mencionar a moeda BRL"),
                    () -> assertTrue(exceptionBRL.getMessage().toLowerCase().contains("saldo insuficiente"),
                            "Mensagem deve indicar saldo insuficiente")
            );

            // When & Then - Tentar debitar valor maior que saldo USD disponível
            BigDecimal valorExcessivoUSD = new BigDecimal("300.00"); // Maior que $ 200,00
            SaldoInsuficienteException exceptionUSD = assertThrows(
                    SaldoInsuficienteException.class,
                    () -> carteira.debitar(valorExcessivoUSD, "USD"),
                    "Deve lançar exceção ao tentar debitar $ 300,00 quando saldo USD é $ 200,00"
            );

            assertAll("Validação da exceção para saldo USD insuficiente",
                    () -> assertTrue(exceptionUSD.getMessage().contains("USD"),
                            "Mensagem deve mencionar a moeda USD"),
                    () -> assertTrue(exceptionUSD.getMessage().toLowerCase().contains("saldo insuficiente"),
                            "Mensagem deve indicar saldo insuficiente")
            );

            // Verificar que os saldos permanecem inalterados após tentativas de débito com falha
            assertAll("Saldos devem permanecer inalterados após tentativas de débito com falha",
                    () -> assertEquals(new BigDecimal("1000.00"), carteira.getSaldoBRL(),
                            "Saldo BRL deve permanecer R$ 1.000,00"),
                    () -> assertEquals(new BigDecimal("200.00"), carteira.getSaldoUSD(),
                            "Saldo USD deve permanecer $ 200,00")
            );
        }


        @Test
        @DisplayName("Deve creditar valor corretamente no saldo em BRL")
        void deveCreditarValorCorretamenteNoSaldoBRL() {
            Carteira carteira = Carteira.builder()
                    .saldoBRL(new BigDecimal("1000.00"))
                    .saldoUSD(new BigDecimal("500.00"))
                    .build();

            carteira.creditar(new BigDecimal("300.00"), "BRL");

            assertEquals(new BigDecimal("1300.00"), carteira.getSaldoBRL());
            assertEquals(new BigDecimal("500.00"), carteira.getSaldoUSD(), "O saldo em USD não deve ser alterado");
        }

        @Test
        @DisplayName("Deve creditar valor corretamente no saldo em USD")
        void deveCreditarValorCorretamenteNoSaldoUSD() {
            Carteira carteira = Carteira.builder()
                    .saldoBRL(new BigDecimal("1000.00"))
                    .saldoUSD(new BigDecimal("500.00"))
                    .build();

            carteira.creditar(new BigDecimal("200.00"), "USD");

            assertEquals(new BigDecimal("1000.00"), carteira.getSaldoBRL(), "O saldo em BRL não deve ser alterado");
            assertEquals(new BigDecimal("700.00"), carteira.getSaldoUSD());
        }

        @Test
        @DisplayName("Deve lançar exceção ao debitar ou creditar com moeda inválida")
        void deveLancarExcecaoComMoedaInvalida() {
            Carteira carteira = Carteira.builder()
                    .saldoBRL(new BigDecimal("1000.00"))
                    .saldoUSD(new BigDecimal("500.00"))
                    .build();

            assertThrows(IllegalArgumentException.class, () -> carteira.debitar(new BigDecimal("100.00"), "EUR"));
            assertThrows(IllegalArgumentException.class, () -> carteira.creditar(new BigDecimal("100.00"), "EUR"));
        }
    }
}