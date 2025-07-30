package com.guilherme.desafiointer.domain;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes da entidade Usuario")
class UsuarioTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static final String NOME_VALIDO = "João Silva";
    private static final String EMAIL_VALIDO = "joao.silva@email.com";
    private static final String DOCUMENTO_PF_VALIDO = "529.982.247-25";
    private static final String DOCUMENTO_PJ_VALIDO = "63.220.504/0001-40";
    private static final String SENHA_VALIDA = "Senha@123";

    @Nested
    @DisplayName("Testes de validação de campos obrigatórios")
    class ValidacaoCamposObrigatoriosTests {

        @Test
        @DisplayName("Deve criar usuário válido")
        void deveCriarUsuarioValido() {
            Usuario usuario = criarUsuarioPF();
            Set<ConstraintViolation<Usuario>> violations = validator.validate(usuario);
            assertTrue(violations.isEmpty());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        @DisplayName("Deve validar nome em branco")
        void deveValidarNomeEmBranco(String nomeInvalido) {
            Usuario usuario = criarUsuarioPFBuilder()
                    .nomeCompleto(nomeInvalido)
                    .build();

            Set<ConstraintViolation<Usuario>> violations = validator.validate(usuario);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("nomeCompleto")));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   ", "email_invalido", "email@", "@dominio.com"})
        @DisplayName("Deve validar email inválido")
        void deveValidarEmailInvalido(String emailInvalido) {
            Usuario usuario = criarUsuarioPFBuilder()
                    .email(emailInvalido)
                    .build();

            Set<ConstraintViolation<Usuario>> violations = validator.validate(usuario);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("email")));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        @DisplayName("Deve validar documento em branco")
        void deveValidarDocumentoEmBranco(String documentoInvalido) {
            Usuario usuario = criarUsuarioPFBuilder()
                    .documento(documentoInvalido)
                    .build();

            Set<ConstraintViolation<Usuario>> violations = validator.validate(usuario);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("documento")));
        }

        @Test
        @DisplayName("Deve validar tipo de usuário nulo")
        void deveValidarTipoUsuarioNulo() {
            Usuario usuario = criarUsuarioPFBuilder()
                    .tipoUsuario(null)
                    .build();

            Set<ConstraintViolation<Usuario>> violations = validator.validate(usuario);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("tipoUsuario")));
        }
    }

    @Nested
    @DisplayName("Testes de senha")
    class SenhaTests {

        @Test
        @DisplayName("Deve definir senha encodada")
        void deveDefinirSenhaEncodada() {
            Usuario usuario = criarUsuarioPF();
            String senhaEncodada = "$2a$10$XXXXXXXXXXXXXXXXXXXXX";

            usuario.setSenhaEncoded(senhaEncodada);

            assertEquals(senhaEncodada, usuario.getSenha());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        @DisplayName("Deve validar senha em branco")
        void deveValidarSenhaEmBranco(String senhaInvalida) {
            Usuario usuario = criarUsuarioPFBuilder()
                    .senha(senhaInvalida)
                    .build();

            Set<ConstraintViolation<Usuario>> violations = validator.validate(usuario);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("senha")));
        }
    }

    @Nested
    @DisplayName("Testes de carteira")
    class CarteiraTests {

        @Test
        @DisplayName("Deve associar carteira ao usuário")
        void deveAssociarCarteiraAoUsuario() {
            Usuario usuario = criarUsuarioPF();
            Carteira carteira = new Carteira();

            usuario.setCarteira(carteira);

            assertNotNull(usuario.getCarteira());
            assertEquals(carteira, usuario.getCarteira());
        }
    }

    @Test
    @DisplayName("Deve gerar toString correto")
    void deveGerarToStringCorreto() {
        Usuario usuario = criarUsuarioPF();
        String toStringEsperado = "Usuario{" +
                "id=" + usuario.getId() +
                ", nomeCompleto='" + usuario.getNomeCompleto() + '\'' +
                ", email='" + usuario.getEmail() + '\'' +
                ", documento='" + usuario.getDocumento() + '\'' +
                ", tipoUsuario=" + usuario.getTipoUsuario() +
                '}';

        assertEquals(toStringEsperado, usuario.toString());
    }

    @Test
    @DisplayName("Deve validar equals e hashCode")
    void deveValidarEqualsEHashCode() {
        Usuario usuario1 = criarUsuarioPFBuilder().id(1L).build();
        Usuario usuario2 = criarUsuarioPFBuilder().id(1L).build();
        Usuario usuario3 = criarUsuarioPFBuilder().id(2L).build();

        assertAll("Verificação de equals e hashCode",
                () -> assertEquals(usuario1, usuario2),
                () -> assertNotEquals(usuario1, usuario3),
                () -> assertEquals(usuario1.hashCode(), usuario2.hashCode()),
                () -> assertNotEquals(usuario1.hashCode(), usuario3.hashCode())
        );
    }

    private Usuario criarUsuarioPF() {
        return criarUsuarioPFBuilder().build();
    }

    private Usuario.UsuarioBuilder criarUsuarioPFBuilder() {
        return Usuario.builder()
                .id(1L)
                .nomeCompleto(NOME_VALIDO)
                .email(EMAIL_VALIDO)
                .documento(DOCUMENTO_PF_VALIDO)
                .senha(SENHA_VALIDA)
                .tipoUsuario(TipoUsuario.PF);
    }

    private Usuario criarUsuarioPJ() {
        return Usuario.builder()
                .id(1L)
                .nomeCompleto("Empresa LTDA")
                .email("contato@empresa.com")
                .documento(DOCUMENTO_PJ_VALIDO)
                .senha(SENHA_VALIDA)
                .tipoUsuario(TipoUsuario.PJ)
                .build();
    }
}