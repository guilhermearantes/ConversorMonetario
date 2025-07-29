package com.guilherme.desafiointer.service;

import com.guilherme.desafiointer.domain.Carteira;
import com.guilherme.desafiointer.domain.Usuario;
import com.guilherme.desafiointer.dto.UsuarioRequestDTO;
import com.guilherme.desafiointer.dto.UsuarioResponseDTO;
import com.guilherme.desafiointer.exception.remessa.RemessaErrorType;
import com.guilherme.desafiointer.exception.remessa.RemessaException;
import com.guilherme.desafiointer.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Serviço responsável pelo gerenciamento de usuários do sistema.
 * Implementa operações CRUD e regras de negócio específicas para usuários.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Usuario criar(UsuarioRequestDTO dto) {
        validarUsuarioUnico(dto);

        try {
            Usuario usuario = Usuario.builder()
                    .nomeCompleto(dto.getNomeCompleto())
                    .email(dto.getEmail())
                    .senha(passwordEncoder.encode(dto.getSenha()))
                    .tipoUsuario(dto.getTipoUsuario())
                    .documento(dto.getDocumento())
                    .build();

            Carteira carteira = Carteira.builder()
                    .usuario(usuario)
                    .saldo(BigDecimal.ZERO)
                    .build();

            usuario.setCarteira(carteira);
            return usuarioRepository.save(usuario);
        } catch (Exception e) {
            throw RemessaException.processamento(
                    RemessaErrorType.ERRO_PROCESSAMENTO_USUARIO,
                    "Erro ao criar usuário",
                    e
            );
        }
    }

    private void validarUsuarioUnico(UsuarioRequestDTO dto) {
        if (usuarioRepository.existsByDocumento(dto.getDocumento())) {
            throw RemessaException.validacao(
                    RemessaErrorType.DOCUMENTO_JA_CADASTRADO,
                    "Documento já cadastrado: " + dto.getDocumento()
            );
        }

        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw RemessaException.validacao(
                    RemessaErrorType.EMAIL_JA_CADASTRADO,
                    "Email já cadastrado: " + dto.getEmail()
            );
        }
    }

    @Transactional
    public void alterarSenha(Long userId, String senhaAtual, String novaSenha) {
        Usuario usuario = buscarPorId(userId);

        if (!passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
            throw RemessaException.validacao(
                    RemessaErrorType.SENHA_INVALIDA,
                    "Senha atual incorreta"
            );
        }

        try {
            usuario.setSenhaEncoded(passwordEncoder.encode(novaSenha));
            usuarioRepository.save(usuario);
        } catch (Exception e) {
            throw RemessaException.processamento(
                    RemessaErrorType.ERRO_PROCESSAMENTO_USUARIO,
                    "Erro ao alterar senha",
                    e
            );
        }
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> RemessaException.negocio(
                        RemessaErrorType.USUARIO_NAO_ENCONTRADO,
                        "Usuário não encontrado com ID: " + id
                ));
    }

    public Usuario buscarPorDocumento(String documento) {
        return usuarioRepository.findByDocumento(documento)
                .orElseThrow(() -> RemessaException.negocio(
                        RemessaErrorType.USUARIO_NAO_ENCONTRADO,
                        "Usuário não encontrado com documento: " + documento
                ));
    }

    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> RemessaException.negocio(
                        RemessaErrorType.USUARIO_NAO_ENCONTRADO,
                        "Usuário não encontrado com email: " + email
                ));
    }

    public List<UsuarioResponseDTO> listarTodos() {
        try {
            return usuarioRepository.findAll().stream()
                    .map(this::converterParaDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw RemessaException.processamento(
                    RemessaErrorType.ERRO_PROCESSAMENTO_USUARIO,
                    "Erro ao listar usuários",
                    e
            );
        }
    }

    @Transactional
    public void deletar(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw RemessaException.negocio(
                    RemessaErrorType.USUARIO_NAO_ENCONTRADO,
                    "Usuário não encontrado com ID: " + id
            );
        }
        try {
            usuarioRepository.deleteById(id);
        } catch (Exception e) {
            throw RemessaException.processamento(
                    RemessaErrorType.ERRO_PROCESSAMENTO_USUARIO,
                    "Erro ao deletar usuário",
                    e
            );
        }
    }

    @Transactional
    public UsuarioResponseDTO atualizar(Long id, UsuarioRequestDTO dto) {
        Usuario usuario = buscarPorId(id);

        validarAtualizacaoUsuario(usuario, dto);

        try {
            usuario = Usuario.builder()
                    .id(usuario.getId())
                    .nomeCompleto(dto.getNomeCompleto())
                    .email(dto.getEmail())
                    .senha(usuario.getSenha())
                    .tipoUsuario(dto.getTipoUsuario())
                    .documento(dto.getDocumento())
                    .carteira(usuario.getCarteira())
                    .build();

            return converterParaDTO(usuarioRepository.save(usuario));
        } catch (Exception e) {
            throw RemessaException.processamento(
                    RemessaErrorType.ERRO_PROCESSAMENTO_USUARIO,
                    "Erro ao atualizar usuário",
                    e
            );
        }
    }

    private void validarAtualizacaoUsuario(Usuario usuario, UsuarioRequestDTO dto) {
        if (!usuario.getEmail().equals(dto.getEmail()) &&
                usuarioRepository.existsByEmail(dto.getEmail())) {
            throw RemessaException.validacao(
                    RemessaErrorType.EMAIL_JA_CADASTRADO,
                    "Email já cadastrado para outro usuário: " + dto.getEmail()
            );
        }

        if (!usuario.getDocumento().equals(dto.getDocumento()) &&
                usuarioRepository.existsByDocumento(dto.getDocumento())) {
            throw RemessaException.validacao(
                    RemessaErrorType.DOCUMENTO_JA_CADASTRADO,
                    "Documento já cadastrado para outro usuário: " + dto.getDocumento()
            );
        }
    }

    private UsuarioResponseDTO converterParaDTO(Usuario usuario) {
        return UsuarioResponseDTO.builder()
                .id(usuario.getId())
                .nomeCompleto(usuario.getNomeCompleto())
                .email(usuario.getEmail())
                .tipoUsuario(usuario.getTipoUsuario())
                .documento(usuario.getDocumento())
                .saldo(usuario.getCarteira() != null ? usuario.getCarteira().getSaldo() : BigDecimal.ZERO)
                .build();
    }
}