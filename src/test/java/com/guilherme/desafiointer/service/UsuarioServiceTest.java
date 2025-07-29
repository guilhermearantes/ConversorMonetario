
package com.guilherme.desafiointer.service;

import com.guilherme.desafiointer.domain.Carteira;
import com.guilherme.desafiointer.domain.TipoUsuario;
import com.guilherme.desafiointer.domain.Usuario;
import com.guilherme.desafiointer.dto.UsuarioRequestDTO;
import com.guilherme.desafiointer.dto.UsuarioResponseDTO;
import com.guilherme.desafiointer.exception.remessa.RemessaErrorType;
import com.guilherme.desafiointer.exception.remessa.RemessaException;
import com.guilherme.desafiointer.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
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
    private static final String SENHA_CURTA = "123";

    @Test
    @DisplayName("Deve criar usuário com dados completos")
    void deveCriarUsuarioComDadosCompletos() {
        // Arrange
        var usuarioDTO = criarUsuarioRequestDTO();
        when(passwordEncoder.encode(any())).thenReturn(SENHA_ENCODED);
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        Usuario usuario = usuarioService.criar(usuarioDTO);

        // Assert
        assertAll("Verificação dos dados do usuário",
                () -> assertEquals(usuarioDTO.getNomeCompleto(), usuario.getNomeCompleto()),
                () -> assertEquals(usuarioDTO.getEmail(), usuario.getEmail()),
                () -> assertEquals(usuarioDTO.getTipoUsuario(), usuario.getTipoUsuario()),
                () -> assertEquals(usuarioDTO.getDocumento(), usuario.getDocumento()),
                () -> assertNotNull(usuario.getCarteira(), "Carteira deve ser criada"),
                () -> assertEquals(BigDecimal.ZERO, usuario.getCarteira().getSaldo())
        );
    }

    @Test
    @DisplayName("Deve impedir criação de usuário com email duplicado")
    void deveImpedirCriacaoUsuarioEmailDuplicado() {
        // Arrange
        var usuarioDTO = criarUsuarioRequestDTO();
        when(usuarioRepository.existsByEmail(usuarioDTO.getEmail())).thenReturn(true);

        // Act & Assert
        RemessaException exception = assertThrows(RemessaException.class,
                () -> usuarioService.criar(usuarioDTO));

        assertAll("Verificação da exceção",
                () -> assertEquals(RemessaErrorType.EMAIL_JA_CADASTRADO, exception.getErrorType()),
                () -> assertTrue(exception.getDetail().contains(usuarioDTO.getEmail()))
        );
    }

    @Test
    @DisplayName("Deve impedir criação de usuário com documento duplicado")
    void deveImpedirCriacaoUsuarioDocumentoDuplicado() {
        // Arrange
        var usuarioDTO = criarUsuarioRequestDTO();
        when(usuarioRepository.existsByDocumento(usuarioDTO.getDocumento())).thenReturn(true);

        // Act & Assert
        RemessaException exception = assertThrows(RemessaException.class,
                () -> usuarioService.criar(usuarioDTO));

        assertAll("Verificação da exceção",
                () -> assertEquals(RemessaErrorType.DOCUMENTO_JA_CADASTRADO, exception.getErrorType()),
                () -> assertTrue(exception.getDetail().contains(usuarioDTO.getDocumento()))
        );
    }

    @Test
    @DisplayName("Deve criar usuário com senha criptografada")
    void deveCriarUsuarioComSenhaCriptografada() {
        // Arrange
        var usuarioDTO = criarUsuarioRequestDTO();
        when(passwordEncoder.encode(SENHA_VALIDA)).thenReturn(SENHA_ENCODED);
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        Usuario usuario = usuarioService.criar(usuarioDTO);

        // Assert
        assertAll("Verificação da senha",
                () -> assertNotEquals(SENHA_VALIDA, usuario.getSenha(),
                        "A senha não deve ser armazenada em texto puro"),
                () -> assertEquals(SENHA_ENCODED, usuario.getSenha(),
                        "A senha deve estar criptografada"),
                () -> verify(passwordEncoder).encode(SENHA_VALIDA)
        );
    }

    @Test
    @DisplayName("Deve alterar senha com sucesso")
    void deveAlterarSenhaComSucesso() {
        // Arrange
        var usuario = criarUsuario();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(SENHA_VALIDA, SENHA_ENCODED)).thenReturn(true);
        when(passwordEncoder.encode(SENHA_NOVA)).thenReturn("$2a$10$NOVOENCODEDPASSWORD");

        // Act
        usuarioService.alterarSenha(1L, SENHA_VALIDA, SENHA_NOVA);

        // Assert
        verify(passwordEncoder).matches(SENHA_VALIDA, SENHA_ENCODED);
        verify(passwordEncoder).encode(SENHA_NOVA);
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário não encontrado")
    void deveLancarExcecaoQuandoUsuarioNaoEncontrado() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        RemessaException exception = assertThrows(RemessaException.class,
                () -> usuarioService.alterarSenha(1L, SENHA_VALIDA, SENHA_NOVA));

        assertAll("Verificação da exceção",
                () -> assertEquals(RemessaErrorType.USUARIO_NAO_ENCONTRADO, exception.getErrorType()),
                () -> assertTrue(exception.getDetail().contains("1"))
        );
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar alterar senha com senha atual incorreta")
    void deveLancarExcecaoAoAlterarSenhaComSenhaAtualIncorreta() {
        // Arrange
        var usuario = criarUsuario();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(SENHA_VALIDA, SENHA_ENCODED)).thenReturn(false);

        // Act & Assert
        RemessaException exception = assertThrows(RemessaException.class,
                () -> usuarioService.alterarSenha(1L, SENHA_VALIDA, SENHA_NOVA));

        assertAll("Verificação da exceção",
                () -> assertEquals(RemessaErrorType.SENHA_INVALIDA, exception.getErrorType()),
                () -> assertEquals("Senha atual incorreta", exception.getDetail())
        );
    }

    private UsuarioRequestDTO criarUsuarioRequestDTO() {
        return UsuarioRequestDTO.builder()
                .nomeCompleto("Teste")
                .email("teste@teste.com")
                .senha(SENHA_VALIDA)
                .tipoUsuario(TipoUsuario.PF)
                .documento("123.456.789-00")
                .build();
    }

    private Usuario criarUsuario() {
        return Usuario.builder()
                .id(1L)
                .nomeCompleto("Teste")
                .email("teste@teste.com")
                .senha(SENHA_ENCODED)
                .tipoUsuario(TipoUsuario.PF)
                .documento("123.456.789-00")
                .build();
    }
}