package com.guilherme.desafiointer.remessa.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes do TipoUsuario")
class TipoUsuarioTest {

    @Nested
    @DisplayName("Testes de limites")
    class LimitesTests {
        @Test
        @DisplayName("Deve retornar limite correto para PF")
        void deveRetornarLimitePF() {
            assertEquals(
                    new BigDecimal("10000.00"),
                    TipoUsuario.PF.getLimiteDiario()
            );
        }

        @Test
        @DisplayName("Deve retornar limite correto para PJ")
        void deveRetornarLimitePJ() {
            assertEquals(
                    new BigDecimal("50000.00"),
                    TipoUsuario.PJ.getLimiteDiario()
            );
        }

        @Test
        @DisplayName("Deve identificar valor que excede limite PF")
        void deveIdentificarExcessoLimitePF() {
            assertTrue(TipoUsuario.PF.excedeLimite(new BigDecimal("10000.01")));
            assertFalse(TipoUsuario.PF.excedeLimite(new BigDecimal("10000.00")));
            assertFalse(TipoUsuario.PF.excedeLimite(new BigDecimal("9999.99")));
        }

        @Test
        @DisplayName("Deve identificar valor que excede limite PJ")
        void deveIdentificarExcessoLimitePJ() {
            assertTrue(TipoUsuario.PJ.excedeLimite(new BigDecimal("50000.01")));
            assertFalse(TipoUsuario.PJ.excedeLimite(new BigDecimal("50000.00")));
            assertFalse(TipoUsuario.PJ.excedeLimite(new BigDecimal("49999.99")));
        }
    }

    @Nested
    @DisplayName("Testes de validação de documento")
    class ValidacaoDocumentoTests {
        @Test
        @DisplayName("Deve validar tamanho correto de CPF")
        void deveValidarTamanhoCPF() {
            assertTrue(TipoUsuario.PF.validaTamanhoDocumento("52998224725"));
            assertTrue(TipoUsuario.PF.validaTamanhoDocumento("529.982.247-25"));
            assertFalse(TipoUsuario.PF.validaTamanhoDocumento("5299822472"));
            assertFalse(TipoUsuario.PF.validaTamanhoDocumento("529982247255"));
        }

        @Test
        @DisplayName("Deve validar tamanho correto de CNPJ")
        void deveValidarTamanhoCNPJ() {
            assertTrue(TipoUsuario.PJ.validaTamanhoDocumento("45997418000153"));
            assertTrue(TipoUsuario.PJ.validaTamanhoDocumento("45.997.418/0001-53"));
            assertFalse(TipoUsuario.PJ.validaTamanhoDocumento("4599741800015"));
            assertFalse(TipoUsuario.PJ.validaTamanhoDocumento("459974180001533"));
        }

        @Test
        @DisplayName("Deve lidar com documento nulo")
        void deveLidarComDocumentoNulo() {
            assertFalse(TipoUsuario.PF.validaTamanhoDocumento(null));
            assertFalse(TipoUsuario.PJ.validaTamanhoDocumento(null));
        }
    }

    @Nested
    @DisplayName("Testes de descrição")
    class DescricaoTests {
        @Test
        @DisplayName("Deve retornar descrição correta")
        void deveRetornarDescricaoCorreta() {
            assertEquals("Pessoa Física", TipoUsuario.PF.getDescricao());
            assertEquals("Pessoa Jurídica", TipoUsuario.PJ.getDescricao());
        }

        @Test
        @DisplayName("ToString deve retornar descrição")
        void toStringShouldReturnDescricao() {
            assertEquals("Pessoa Física", TipoUsuario.PF.toString());
            assertEquals("Pessoa Jurídica", TipoUsuario.PJ.toString());
        }
    }
}