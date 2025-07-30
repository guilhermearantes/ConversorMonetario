
package com.guilherme.desafiointer.service;

import com.guilherme.desafiointer.domain.TipoUsuario;
import com.guilherme.desafiointer.domain.Usuario;
import com.guilherme.desafiointer.exception.remessa.RemessaErrorType;
import com.guilherme.desafiointer.exception.remessa.RemessaException;
import com.guilherme.desafiointer.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Testes do UsuarioService")
class UsuarioServiceTest {

    @Autowired
    private UsuarioService usuarioService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    private static final String SENHA_VALIDA = "Senha@123";
    private static final String SENHA_NOVA = "NovaSenha@123";
    private static final String SENHA_ENCODED = "$2a$10$XXXXXXXXXXXXXXXXXXXXX";
    private static final String NOME_COMPLETO = "Teste Usuario";
    private static final String EMAIL = "teste@teste.com";
    private static final String DOCUMENTO = "123.456.789-00";
    private static final TipoUsuario TIPO_USUARIO = TipoUsuario.PF;

    @Nested
    @DisplayName("Testes de busca de usuário")
    class BuscaUsuarioTests {

        @Test
        @DisplayName("Deve buscar usuário por ID com sucesso")
        void deveBuscarUsuarioPorIdComSucesso() {
            Usuario usuarioEsperado = criarUsuario();
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioEsperado));

            Usuario usuarioEncontrado = usuarioService.buscarPorId(1L);

            assertEquals(usuarioEsperado, usuarioEncontrado);
        }

        @Test
        @DisplayName("Deve lançar exceção quando usuário não encontrado por ID")
        void deveLancarExcecaoQuandoUsuarioNaoEncontradoPorId() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

            RemessaException exception = assertThrows(RemessaException.class,
                    () -> usuarioService.buscarPorId(1L));

            assertExcecaoUsuarioNaoEncontrado(exception);
        }

        @Test
        @DisplayName("Deve buscar usuário por documento com sucesso")
        void deveBuscarUsuarioPorDocumentoComSucesso() {
            Usuario usuarioEsperado = criarUsuario();
            when(usuarioRepository.findByDocumento(DOCUMENTO)).thenReturn(Optional.of(usuarioEsperado));

            Usuario usuarioEncontrado = usuarioService.buscarPorDocumento(DOCUMENTO);

            assertEquals(usuarioEsperado, usuarioEncontrado);
        }

        @Test
        @DisplayName("Deve lançar exceção quando usuário não encontrado por documento")
        void deveLancarExcecaoQuandoUsuarioNaoEncontradoPorDocumento() {
            when(usuarioRepository.findByDocumento(DOCUMENTO)).thenReturn(Optional.empty());

            RemessaException exception = assertThrows(RemessaException.class,
                    () -> usuarioService.buscarPorDocumento(DOCUMENTO));

            assertExcecaoUsuarioNaoEncontrado(exception);
        }
    }

    @Nested
    @DisplayName("Testes de alteração de senha")
    class AlteracaoSenhaTests {

        @Test
        @DisplayName("Deve alterar senha com sucesso")
        void deveAlterarSenhaComSucesso() {
            Usuario usuario = criarUsuario();
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches(SENHA_VALIDA, SENHA_ENCODED)).thenReturn(true);
            when(passwordEncoder.encode(SENHA_NOVA)).thenReturn("$2a$10$NOVOENCODEDPASSWORD");

            usuarioService.alterarSenha(1L, SENHA_VALIDA, SENHA_NOVA);

            verify(passwordEncoder).matches(SENHA_VALIDA, SENHA_ENCODED);
            verify(passwordEncoder).encode(SENHA_NOVA);
            verify(usuarioRepository).save(any(Usuario.class));
        }

        @Test
        @DisplayName("Deve lançar exceção ao alterar senha com senha atual incorreta")
        void deveLancarExcecaoAoAlterarSenhaComSenhaAtualIncorreta() {
            Usuario usuario = criarUsuario();
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches(SENHA_VALIDA, SENHA_ENCODED)).thenReturn(false);

            RemessaException exception = assertThrows(RemessaException.class,
                    () -> usuarioService.alterarSenha(1L, SENHA_VALIDA, SENHA_NOVA));

            assertAll("Verificação da exceção",
                    () -> assertEquals(RemessaErrorType.SENHA_INVALIDA, exception.getErrorType()),
                    () -> assertEquals("Senha atual incorreta", exception.getDetail())
            );
        }

        @Test
        @DisplayName("Deve lançar exceção ao alterar senha de usuário inexistente")
        void deveLancarExcecaoAoAlterarSenhaDeUsuarioInexistente() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

            RemessaException exception = assertThrows(RemessaException.class,
                    () -> usuarioService.alterarSenha(1L, SENHA_VALIDA, SENHA_NOVA));

            assertExcecaoUsuarioNaoEncontrado(exception);
        }
    }

    private Usuario criarUsuario() {
        return Usuario.builder()
                .id(1L)
                .nomeCompleto(NOME_COMPLETO)
                .email(EMAIL)
                .senha(SENHA_ENCODED)
                .tipoUsuario(TIPO_USUARIO)
                .documento(DOCUMENTO)
                .build();
    }

    private void assertExcecaoUsuarioNaoEncontrado(RemessaException exception) {
        assertAll("Verificação da exceção de usuário não encontrado",
                () -> assertEquals(RemessaErrorType.USUARIO_NAO_ENCONTRADO, exception.getErrorType()),
                () -> assertNotNull(exception.getDetail())
        );
    }
}