
package com.guilherme.desafiointer.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes do enum TipoUsuario")
class TipoUsuarioTest {

    @Nested
    @DisplayName("Testes de validação de documento")
    class ValidacaoDocumentoTests {

        @Test
        @DisplayName("Deve validar CPF corretamente")
        void deveValidarCPFCorretamente() {
            String cpfValido = "529.982.247-25";
            assertTrue(TipoUsuario.PF.validarDocumento(cpfValido));

            String cpfInvalido = "111.111.111-11";
            assertFalse(TipoUsuario.PF.validarDocumento(cpfInvalido));
        }

        @Test
        @DisplayName("Deve validar CNPJ corretamente")
        void deveValidarCNPJCorretamente() {
            String cnpjValido = "45.997.418/0001-53";
            assertTrue(TipoUsuario.PJ.validarDocumento(cnpjValido));

            String cnpjInvalido = "11.111.111/1111-11";
            assertFalse(TipoUsuario.PJ.validarDocumento(cnpjInvalido));
        }

        @Test
        @DisplayName("Deve retornar false para documento null")
        void deveRetornarFalseParaDocumentoNull() {
            assertFalse(TipoUsuario.PF.validarDocumento(null));
            assertFalse(TipoUsuario.PJ.validarDocumento(null));
        }
    }

    @Nested
    @DisplayName("Testes de formato de documento")
    class FormatoDocumentoTests {

        @Test
        @DisplayName("Deve retornar formato correto para CPF")
        void deveRetornarFormatoCorretoCPF() {
            assertEquals("CPF deve conter 11 dígitos numéricos",
                    TipoUsuario.PF.getFormatoDocumento());
        }

        @Test
        @DisplayName("Deve retornar formato correto para CNPJ")
        void deveRetornarFormatoCorretoCNPJ() {
            assertEquals("CNPJ deve conter 14 dígitos numéricos",
                    TipoUsuario.PJ.getFormatoDocumento());
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
    }

    @Nested
    @DisplayName("Testes de tamanho do documento")
    class TamanhoDocumentoTests {

        @Test
        @DisplayName("Deve retornar tamanho correto do documento")
        void deveRetornarTamanhoCorretoDocumento() {
            assertEquals(11, TipoUsuario.PF.getTamanhoDocumento());
            assertEquals(14, TipoUsuario.PJ.getTamanhoDocumento());
        }
    }
}