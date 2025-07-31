package com.guilherme.desafiointer.repository;

import com.guilherme.desafiointer.domain.TipoUsuario;
import com.guilherme.desafiointer.domain.Usuario;
import com.guilherme.desafiointer.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import com.guilherme.desafiointer.exception.remessa.RemessaErrorType;
import com.guilherme.desafiointer.exception.remessa.RemessaException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import java.util.Optional;
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
    private static final String SENHA_ENCODED = "$2a$10$XXXXXXXXXXXXX";
    private static final String NOME_COMPLETO = "Teste Usuario";
    private static final String EMAIL = "teste@teste.com";
    private static final String DOCUMENTO = "123.456.789-00";
    private static final TipoUsuario TIPO_USUARIO = TipoUsuario.PF;

    @Nested
    @DisplayName("Testes de criação de usuário")
    class CriacaoUsuarioTests {

        @Test
        @DisplayName("Deve salvar um usuário PF com carteira inicializada corretamente")
        void deveSalvarUsuarioPFComCarteira() {
            // Arrange
            Usuario usuario = Usuario.builder()
                    .nomeCompleto(NOME_COMPLETO)
                    .email(EMAIL)
                    .senha(SENHA_VALIDA)
                    .tipoUsuario(TIPO_USUARIO)
                    .documento(DOCUMENTO)
                    .build();

            when(passwordEncoder.encode(SENHA_VALIDA)).thenReturn(SENHA_ENCODED);
            when(usuarioRepository.save(any(Usuario.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Usuario usuarioSalvo = usuarioService.criarUsuario(
                    usuario.getNomeCompleto(),
                    usuario.getEmail(),
                    usuario.getSenha(),
                    usuario.getTipoUsuario(),
                    usuario.getDocumento()
            );

            // Assert
            assertNotNull(usuarioSalvo);
            assertNotNull(usuarioSalvo.getCarteira());
            assertAll("Verificando carteira inicializada",
                    () -> assertEquals(BigDecimal.ZERO, usuarioSalvo.getCarteira().getSaldoBRL()),
                    () -> assertEquals(BigDecimal.ZERO, usuarioSalvo.getCarteira().getSaldoUSD())
            );

            verify(usuarioRepository, times(1)).save(any(Usuario.class));
            verify(passwordEncoder, times(1)).encode(SENHA_VALIDA);
        }

        @Test
        @DisplayName("Deve lançar exceção ao tentar salvar usuário com email já cadastrado")
        void deveLancarExcecaoParaEmailDuplicado() {
            // Arrange
            when(usuarioRepository.existsByEmail(EMAIL)).thenReturn(true);

            // Act & Assert
            RemessaException exception = assertThrows(RemessaException.class, () ->
                    usuarioService.criarUsuario(
                            NOME_COMPLETO, EMAIL, SENHA_VALIDA, TIPO_USUARIO, DOCUMENTO
                    )
            );

            assertEquals(RemessaErrorType.EMAIL_JA_CADASTRADO, exception.getErrorType());
        }

        @Test
        @DisplayName("Deve lançar exceção ao tentar salvar usuário com documento já cadastrado")
        void deveLancarExcecaoParaDocumentoDuplicado() {
            // Arrange
            when(usuarioRepository.existsByDocumento(DOCUMENTO)).thenReturn(true);

            // Act & Assert
            RemessaException exception = assertThrows(RemessaException.class, () ->
                    usuarioService.criarUsuario(
                            NOME_COMPLETO, EMAIL, SENHA_VALIDA, TIPO_USUARIO, DOCUMENTO
                    )
            );

            assertEquals(RemessaErrorType.DOCUMENTO_JA_CADASTRADO, exception.getErrorType());
        }
    }

    @Nested
    @DisplayName("Testes de busca de usuários")
    class BuscaUsuarioTests {

        @Test
        @DisplayName("Deve buscar usuário existente por documento")
        void deveBuscarUsuarioPorDocumento() {
            // Arrange
            Usuario usuario = criarUsuario();
            when(usuarioRepository.findByDocumento(DOCUMENTO))
                    .thenReturn(Optional.of(usuario));

            // Act
            Usuario usuarioEncontrado = usuarioService.buscarPorDocumento(DOCUMENTO);

            // Assert
            assertNotNull(usuarioEncontrado);
            assertEquals(DOCUMENTO, usuarioEncontrado.getDocumento());
        }

        @Test
        @DisplayName("Deve lançar exceção ao buscar usuário inexistente por documento")
        void deveLancarExcecaoAoBuscarUsuarioPorDocumentoInexistente() {
            // Arrange
            when(usuarioRepository.findByDocumento(DOCUMENTO)).thenReturn(Optional.empty());

            // Act & Assert
            RemessaException exception = assertThrows(RemessaException.class, () ->
                    usuarioService.buscarPorDocumento(DOCUMENTO)
            );

            assertEquals(RemessaErrorType.USUARIO_NAO_ENCONTRADO, exception.getErrorType());
        }
    }

    @Nested
    @DisplayName("Testes de alteração de senha")
    class AlteracaoSenhaTests {

        @Test
        @DisplayName("Deve alterar senha com sucesso")
        void deveAlterarSenhaComSucesso() {
            // Arrange
            Usuario usuario = criarUsuario();
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches(SENHA_VALIDA, SENHA_ENCODED)).thenReturn(true);
            when(passwordEncoder.encode(SENHA_NOVA)).thenReturn("$2a$10$newpassword");

            // Act
            usuarioService.alterarSenha(1L, SENHA_VALIDA, SENHA_NOVA);

            // Assert
            verify(passwordEncoder, times(1)).matches(SENHA_VALIDA, SENHA_ENCODED);
            verify(passwordEncoder, times(1)).encode(SENHA_NOVA);
            verify(usuarioRepository, times(1)).save(any(Usuario.class));
        }

        @Test
        @DisplayName("Deve lançar exceção ao alterar senha com senha atual incorreta")
        void deveLancarExcecaoParaSenhaAtualInvalida() {
            // Arrange
            Usuario usuario = criarUsuario();
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches(SENHA_VALIDA, SENHA_ENCODED)).thenReturn(false);

            // Act & Assert
            RemessaException exception = assertThrows(RemessaException.class, () ->
                    usuarioService.alterarSenha(1L, SENHA_VALIDA, SENHA_NOVA)
            );

            assertEquals(RemessaErrorType.SENHA_INVALIDA, exception.getErrorType());
        }

        @Test
        @DisplayName("Deve lançar exceção ao alterar senha de usuário inexistente")
        void deveLancarExcecaoParaUsuarioInexistente() {
            // Arrange
            when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

            // Act & Assert
            RemessaException exception = assertThrows(RemessaException.class, () ->
                    usuarioService.alterarSenha(1L, SENHA_VALIDA, SENHA_NOVA)
            );

            assertEquals(RemessaErrorType.USUARIO_NAO_ENCONTRADO, exception.getErrorType());
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
}