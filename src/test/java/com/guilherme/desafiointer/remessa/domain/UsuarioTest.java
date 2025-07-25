
package com.guilherme.desafiointer.remessa.domain;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes da entidade Usuario")
class UsuarioTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("Testes de criação de usuário válido")
    class CriacaoUsuarioValido {
        @Test
        @DisplayName("Deve criar usuário PF com dados válidos")
        void deveCriarUsuarioValidoPF() {
            Usuario usuario = Usuario.builder()
                    .nomeCompleto("João Silva")
                    .email("joao.silva@email.com")
                    .senha("Senha@123")
                    .tipoUsuario(TipoUsuario.PF)
                    .documento("529.982.247-25")
                    .build();

            var violations = validator.validate(usuario);
            assertTrue(violations.isEmpty(), "Deveria criar usuário PF válido");
        }

        @Test
        @DisplayName("Deve criar usuário PJ com dados válidos")
        void deveCriarUsuarioValidoPJ() {
            Usuario usuario = Usuario.builder()
                    .nomeCompleto("Empresa LTDA")
                    .email("empresa@email.com")
                    .senha("Senha@123")
                    .tipoUsuario(TipoUsuario.PJ)
                    .documento("45.997.418/0001-53")
                    .build();

            var violations = validator.validate(usuario);
            assertTrue(violations.isEmpty(), "Deveria criar usuário PJ válido");
        }
    }

    @Nested
    @DisplayName("Testes de validação de campos obrigatórios")
    class ValidacaoCamposObrigatorios {
        @Test
        @DisplayName("Não deve criar usuário sem nome")
        void naoDeveCriarUsuarioSemNome() {
            Usuario usuario = Usuario.builder()
                    .email("teste@email.com")
                    .senha("Senha@123")
                    .tipoUsuario(TipoUsuario.PF)
                    .documento("529.982.247-25")
                    .build();

            var violations = validator.validate(usuario);
            assertFalse(violations.isEmpty(), "Não deveria criar usuário sem nome");
            assertTrue(violations.stream()
                            .anyMatch(v -> v.getMessage().contains("Nome completo é obrigatório")),
                    "Deveria conter mensagem de erro sobre nome obrigatório");
        }

        @Test
        @DisplayName("Não deve criar usuário sem email")
        void naoDeveCriarUsuarioSemEmail() {
            Usuario usuario = Usuario.builder()
                    .nomeCompleto("João Silva")
                    .senha("Senha@123")
                    .tipoUsuario(TipoUsuario.PF)
                    .documento("529.982.247-25")
                    .build();

            var violations = validator.validate(usuario);
            assertFalse(violations.isEmpty(), "Não deveria criar usuário sem email");
            assertTrue(violations.stream()
                            .anyMatch(v -> v.getMessage().contains("Email é obrigatório")),
                    "Deveria conter mensagem de erro sobre email obrigatório");
        }

        @Test
        @DisplayName("Não deve criar usuário sem senha")
        void naoDeveCriarUsuarioSemSenha() {
            Usuario usuario = Usuario.builder()
                    .nomeCompleto("João Silva")
                    .email("joao@email.com")
                    .tipoUsuario(TipoUsuario.PF)
                    .documento("529.982.247-25")
                    .build();

            var violations = validator.validate(usuario);
            assertFalse(violations.isEmpty(), "Não deveria criar usuário sem senha");
            assertTrue(violations.stream()
                            .anyMatch(v -> v.getMessage().contains("Senha é obrigatória")),
                    "Deveria conter mensagem de erro sobre senha obrigatória");
        }

        @Test
        @DisplayName("Não deve criar usuário sem documento")
        void naoDeveCriarUsuarioSemDocumento() {
            Usuario usuario = Usuario.builder()
                    .nomeCompleto("João Silva")
                    .email("joao@email.com")
                    .senha("Senha@123")
                    .tipoUsuario(TipoUsuario.PF)
                    .build();

            assertThrows(IllegalArgumentException.class,
                    () -> usuario.validarDocumento(),
                    "Deveria lançar exceção quando documento for nulo");
        }

        @Test
        @DisplayName("Não deve criar usuário sem tipo de usuário")
        void naoDeveCriarUsuarioSemTipoUsuario() {
            Usuario usuario = Usuario.builder()
                    .nomeCompleto("João Silva")
                    .email("joao@email.com")
                    .senha("Senha@123")
                    .documento("529.982.247-25")
                    .build();

            assertThrows(IllegalArgumentException.class,
                    () -> usuario.validarDocumento(),
                    "Deveria lançar exceção quando tipo de usuário for nulo");
        }
    }

    @Nested
    @DisplayName("Testes de validação de email")
    class ValidacaoEmail {
        @ParameterizedTest
        @DisplayName("Não deve aceitar emails inválidos")
        @ValueSource(strings = {
                "emailinvalido",
                "email@",
                "@dominio.com",
                "email@dominio.",
                "email.com",
                "@",
                "email@dominio@com"
        })
        void naoDeveCriarUsuarioComEmailInvalido(String emailInvalido) {
            Usuario usuario = Usuario.builder()
                    .nomeCompleto("João Silva")
                    .email(emailInvalido)
                    .senha("Senha@123")
                    .tipoUsuario(TipoUsuario.PF)
                    .documento("529.982.247-25")
                    .build();

            var violations = validator.validate(usuario);
            assertFalse(violations.isEmpty(), "Não deveria criar usuário com email inválido: " + emailInvalido);
            assertTrue(violations.stream()
                            .anyMatch(v -> v.getMessage().contains("Email deve ser válido")),
                    "Deveria conter mensagem de erro sobre email inválido");
        }
    }

    @Nested
    @DisplayName("Testes de validação de documento")
    class ValidacaoDocumento {
        @Test
        @DisplayName("Deve aceitar CPF válido com formatação")
        void deveAceitarCPFValidoFormatado() {
            Usuario usuario = Usuario.builder()
                    .nomeCompleto("João Silva")
                    .email("joao@email.com")
                    .senha("Senha@123")
                    .tipoUsuario(TipoUsuario.PF)
                    .documento("529.982.247-25")
                    .build();

            assertDoesNotThrow(() -> usuario.validarDocumento());
        }

        @Test
        @DisplayName("Deve aceitar CPF válido sem formatação")
        void deveAceitarCPFValidoSemFormatacao() {
            Usuario usuario = Usuario.builder()
                    .nomeCompleto("João Silva")
                    .email("joao@email.com")
                    .senha("Senha@123")
                    .tipoUsuario(TipoUsuario.PF)
                    .documento("52998224725")
                    .build();

            assertDoesNotThrow(() -> usuario.validarDocumento());
        }

        @Test
        @DisplayName("Deve aceitar CNPJ válido com formatação")
        void deveAceitarCNPJValidoFormatado() {
            Usuario usuario = Usuario.builder()
                    .nomeCompleto("Empresa LTDA")
                    .email("empresa@email.com")
                    .senha("Senha@123")
                    .tipoUsuario(TipoUsuario.PJ)
                    .documento("45.997.418/0001-53")
                    .build();

            assertDoesNotThrow(() -> usuario.validarDocumento());
        }

        @Test
        @DisplayName("Deve aceitar CNPJ válido sem formatação")
        void deveAceitarCNPJValidoSemFormatacao() {
            Usuario usuario = Usuario.builder()
                    .nomeCompleto("Empresa LTDA")
                    .email("empresa@email.com")
                    .senha("Senha@123")
                    .tipoUsuario(TipoUsuario.PJ)
                    .documento("45997418000153")
                    .build();

            assertDoesNotThrow(() -> usuario.validarDocumento());
        }

        @Test
        @DisplayName("Não deve aceitar CPF com dígitos iguais")
        void naoDeveAceitarCPFComDigitosIguais() {
            Usuario usuario = Usuario.builder()
                    .nomeCompleto("João Silva")
                    .email("joao@email.com")
                    .senha("Senha@123")
                    .tipoUsuario(TipoUsuario.PF)
                    .documento("111.111.111-11")
                    .build();

            assertThrows(IllegalArgumentException.class,
                    () -> usuario.validarDocumento(),
                    "Não deve aceitar CPF com todos os dígitos iguais");
        }

        @Test
        @DisplayName("Não deve aceitar CNPJ com dígitos iguais")
        void naoDeveAceitarCNPJComDigitosIguais() {
            Usuario usuario = Usuario.builder()
                    .nomeCompleto("Empresa LTDA")
                    .email("empresa@email.com")
                    .senha("Senha@123")
                    .tipoUsuario(TipoUsuario.PJ)
                    .documento("11.111.111/1111-11")
                    .build();

            assertThrows(IllegalArgumentException.class,
                    () -> usuario.validarDocumento(),
                    "Não deve aceitar CNPJ com todos os dígitos iguais");
        }

        @Test
        @DisplayName("Não deve aceitar CPF com dígitos verificadores inválidos")
        void naoDeveAceitarCPFInvalido() {
            Usuario usuario = Usuario.builder()
                    .nomeCompleto("João Silva")
                    .email("joao@email.com")
                    .senha("Senha@123")
                    .tipoUsuario(TipoUsuario.PF)
                    .documento("123.456.789-10")
                    .build();

            assertThrows(IllegalArgumentException.class,
                    () -> usuario.validarDocumento(),
                    "Não deve aceitar CPF com dígitos verificadores inválidos");
        }

        @Test
        @DisplayName("Não deve aceitar CNPJ com dígitos verificadores inválidos")
        void naoDeveAceitarCNPJInvalido() {
            Usuario usuario = Usuario.builder()
                    .nomeCompleto("Empresa LTDA")
                    .email("empresa@email.com")
                    .senha("Senha@123")
                    .tipoUsuario(TipoUsuario.PJ)
                    .documento("45.997.418/0001-54")
                    .build();

            assertThrows(IllegalArgumentException.class,
                    () -> usuario.validarDocumento(),
                    "Não deve aceitar CNPJ com dígitos verificadores inválidos");
        }
    }

    @Nested
    @DisplayName("Testes de carteira")
    class CarteiraTests {
        @Test
        @DisplayName("Deve permitir associar carteira ao usuário")
        void devePermitirAssociarCarteira() {
            Usuario usuario = Usuario.builder()
                    .nomeCompleto("João Silva")
                    .email("joao@email.com")
                    .senha("Senha@123")
                    .tipoUsuario(TipoUsuario.PF)
                    .documento("529.982.247-25")
                    .build();

            Carteira carteira = Carteira.builder()
                    .saldo(BigDecimal.ZERO)
                    .usuario(usuario)
                    .build();

            usuario.setCarteira(carteira);
            assertEquals(carteira, usuario.getCarteira());
        }
    }

    @Nested
    @DisplayName("Testes de métodos da classe Object")
    class MetodosObject {
        @Test
        @DisplayName("Deve implementar equals e hashCode corretamente")
        void deveImplementarEqualsEHashCodeCorretamente() {
            Usuario usuario1 = Usuario.builder().id(1L).build();
            Usuario usuario2 = Usuario.builder().id(1L).build();
            Usuario usuario3 = Usuario.builder().id(2L).build();

            assertEquals(usuario1, usuario2, "Usuários com mesmo ID devem ser iguais");
            assertNotEquals(usuario1, usuario3, "Usuários com IDs diferentes não devem ser iguais");
            assertEquals(usuario1.hashCode(), usuario2.hashCode(), "Hash codes devem ser iguais para mesmo ID");
        }



        @Test
        @DisplayName("Deve gerar toString com informações essenciais do usuário")
        void deveGerarToStringComInformacoesEssenciais() {
            Usuario usuario = Usuario.builder()
                    .id(1L)
                    .nomeCompleto("João Silva")
                    .email("joao@email.com")
                    .tipoUsuario(TipoUsuario.PF)
                    .documento("529.982.247-25")
                    .build();

            String toStringResult = usuario.toString();
            System.out.println("toString resultado: " + toStringResult);

            // Verificação mais flexível da estrutura
            assertTrue(toStringResult.contains("Usuario"), "Deve conter o nome da classe");
            assertTrue(toStringResult.contains(String.valueOf(usuario.getId())), "Deve conter o ID");
            assertTrue(toStringResult.contains(usuario.getNomeCompleto()), "Deve conter o nome completo");
            assertTrue(toStringResult.contains(usuario.getEmail()), "Deve conter o email");
            assertTrue(toStringResult.contains(usuario.getTipoUsuario().toString()), "Deve conter o tipo de usuário");
            assertTrue(toStringResult.contains(usuario.getDocumento()), "Deve conter o documento");
        }

        @Test
        @DisplayName("Deve permitir atualizar senha")
        void devePermitirAtualizarSenha() {
            Usuario usuario = Usuario.builder()
                    .nomeCompleto("João Silva")
                    .email("joao@email.com")
                    .senha("Senha@123")
                    .tipoUsuario(TipoUsuario.PF)
                    .documento("529.982.247-25")
                    .build();

            String novaSenha = "NovaSenha@123";
            usuario.setSenhaEncoded(novaSenha);
            assertEquals(novaSenha, usuario.getSenha());
        }
    }
}