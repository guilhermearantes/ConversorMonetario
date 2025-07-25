package com.guilherme.desafiointer.service;

import com.guilherme.desafiointer.domain.Carteira;
import com.guilherme.desafiointer.domain.Usuario;
import com.guilherme.desafiointer.dto.UsuarioRequestDTO;
import com.guilherme.desafiointer.dto.UsuarioResponseDTO;
import com.guilherme.desafiointer.exception.RemessaException;
import com.guilherme.desafiointer.exception.UsuarioNotFoundException;
import com.guilherme.desafiointer.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Usuario criar(UsuarioRequestDTO dto) {
        validarUsuarioUnico(dto);

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
    }

    private void validarUsuarioUnico(UsuarioRequestDTO dto) {
        if (usuarioRepository.existsByDocumento(dto.getDocumento())) {
            throw new RemessaException("Documento já cadastrado");
        }

        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new RemessaException("Email já cadastrado");
        }
    }

    @Transactional
    public void alterarSenha(Long userId, String senhaAtual, String novaSenha) {
        Usuario usuario = buscarPorId(userId);

        if (!passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
            throw new RemessaException("Senha atual incorreta");
        }

        usuario.setSenhaEncoded(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNotFoundException("Usuário não encontrado"));
    }

    public Usuario buscarPorDocumento(String documento) {
        return usuarioRepository.findByDocumento(documento)
                .orElseThrow(() -> new UsuarioNotFoundException("Usuário não encontrado"));
    }

    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsuarioNotFoundException("Usuário não encontrado"));
    }

    public List<UsuarioResponseDTO> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
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

    @Transactional
    public void deletar(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new UsuarioNotFoundException("Usuário não encontrado");
        }
        usuarioRepository.deleteById(id);
    }

    @Transactional
    public UsuarioResponseDTO atualizar(Long id, UsuarioRequestDTO dto) {
        Usuario usuario = buscarPorId(id);

        // Verifica se o novo email já existe para outro usuário
        if (!usuario.getEmail().equals(dto.getEmail()) &&
                usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new RemessaException("Email já cadastrado para outro usuário");
        }

        // Verifica se o novo documento já existe para outro usuário
        if (!usuario.getDocumento().equals(dto.getDocumento()) &&
                usuarioRepository.existsByDocumento(dto.getDocumento())) {
            throw new RemessaException("Documento já cadastrado para outro usuário");
        }

        usuario = Usuario.builder()
                .id(usuario.getId())
                .nomeCompleto(dto.getNomeCompleto())
                .email(dto.getEmail())
                .senha(usuario.getSenha()) // Mantém a senha atual
                .tipoUsuario(dto.getTipoUsuario())
                .documento(dto.getDocumento())
                .carteira(usuario.getCarteira())
                .build();

        return converterParaDTO(usuarioRepository.save(usuario));
    }
}