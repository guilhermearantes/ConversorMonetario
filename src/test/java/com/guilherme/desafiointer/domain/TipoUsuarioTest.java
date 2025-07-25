package com.guilherme.desafiointer.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes do TipoUsuario")
class TipoUsuarioTest {

    @Nested
    @DisplayName("Testes de limites e taxas")
    class LimitesETaxasTests {
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
        @DisplayName("Deve calcular taxa corretamente para PF")
        void deveCalcularTaxaPF() {
            BigDecimal valor = new BigDecimal("1000.00");
            BigDecimal taxaEsperada = new BigDecimal("20.00"); // 2% de 1000
            assertEquals(taxaEsperada, TipoUsuario.PF.calcularTaxa(valor));
        }

        @Test
        @DisplayName("Deve calcular taxa corretamente para PJ")
        void deveCalcularTaxaPJ() {
            BigDecimal valor = new BigDecimal("1000.00");
            BigDecimal taxaEsperada = new BigDecimal("10.00"); // 1% de 1000
            assertEquals(taxaEsperada, TipoUsuario.PJ.calcularTaxa(valor));
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
        @DisplayName("Deve validar CPF corretamente")
        void deveValidarCPF() {
            assertTrue(TipoUsuario.PF.validarDocumento("52998224725"));
            assertTrue(TipoUsuario.PF.validarDocumento("529.982.247-25"));
            assertFalse(TipoUsuario.PF.validarDocumento("11111111111"));
            assertFalse(TipoUsuario.PF.validarDocumento("12345678910"));
        }

        @Test
        @DisplayName("Deve validar CNPJ corretamente")
        void deveValidarCNPJ() {
            assertTrue(TipoUsuario.PJ.validarDocumento("45997418000153"));
            assertTrue(TipoUsuario.PJ.validarDocumento("45.997.418/0001-53"));
            assertFalse(TipoUsuario.PJ.validarDocumento("11111111111111"));
            assertFalse(TipoUsuario.PJ.validarDocumento("12345678901234"));
        }

        @Test
        @DisplayName("Deve retornar mensagem de formato correta")
        void deveRetornarMensagemFormato() {
            assertEquals("CPF deve conter 11 dígitos numéricos", TipoUsuario.PF.getFormatoDocumento());
            assertEquals("CNPJ deve conter 14 dígitos numéricos", TipoUsuario.PJ.getFormatoDocumento());
        }

        @Test
        @DisplayName("Deve lidar com documento nulo")
        void deveLidarComDocumentoNulo() {
            assertFalse(TipoUsuario.PF.validarDocumento(null));
            assertFalse(TipoUsuario.PJ.validarDocumento(null));
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

    @Nested
    @DisplayName("Testes de tamanho do documento")
    class TamanhoDocumentoTests {
        @Test
        @DisplayName("Deve retornar tamanho correto do documento")
        void deveRetornarTamanhoDocumento() {
            assertEquals(11, TipoUsuario.PF.getTamanhoDocumento());
            assertEquals(14, TipoUsuario.PJ.getTamanhoDocumento());
        }
    }
}