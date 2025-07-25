
package com.guilherme.desafiointer.remessa.service;

import com.guilherme.desafiointer.remessa.domain.Usuario;
import com.guilherme.desafiointer.remessa.domain.TipoUsuario;
import com.guilherme.desafiointer.remessa.dto.UsuarioRequestDTO;
import com.guilherme.desafiointer.remessa.exception.RemessaException;
import com.guilherme.desafiointer.remessa.exception.UsuarioNotFoundException;
import com.guilherme.desafiointer.remessa.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Usuario criar(UsuarioRequestDTO dto) {
        if (usuarioRepository.existsByDocumento(dto.getDocumento())) {
            throw new RemessaException("Documento já cadastrado");
        }

        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new RemessaException("Email já cadastrado");
        }

        String senhaEncoded = passwordEncoder.encode(dto.getSenha());

        Usuario usuario = Usuario.builder()
                .nomeCompleto(dto.getNomeCompleto())
                .email(dto.getEmail())
                .senha(senhaEncoded)
                .tipoUsuario(dto.getTipoUsuario())
                .documento(dto.getDocumento())
                .build();

        return usuarioRepository.save(usuario);
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNotFoundException("Usuário não encontrado"));
    }

    public Usuario buscarPorDocumento(String documento) {
        return usuarioRepository.findByDocumento(documento)
                .orElseThrow(() -> new UsuarioNotFoundException("Usuário não encontrado"));
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
}