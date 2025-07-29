package com.guilherme.desafiointer.controller;

import com.guilherme.desafiointer.domain.Usuario;
import com.guilherme.desafiointer.dto.UsuarioRequestDTO;
import com.guilherme.desafiointer.dto.UsuarioResponseDTO;
import com.guilherme.desafiointer.exception.remessa.RemessaException;
import com.guilherme.desafiointer.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Controller REST responsável por expor endpoints de gerenciamento de usuários.
 * Implementa operações CRUD e funcionalidades específicas do domínio.
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    /**
     * Cria um usuário.
     *
     * @param request DTO com dados do novo usuário
     * @return ResponseEntity<UsuarioResponseDTO> com dados do usuário criado
     * @throws RemessaException se o documento ou email já existirem
     */
    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> criar(@Valid @RequestBody UsuarioRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(converterParaDTO(usuarioService.criar(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(converterParaDTO(usuarioService.buscarPorId(id)));
    }

    @GetMapping("/documento/{documento}")
    public ResponseEntity<UsuarioResponseDTO> buscarPorDocumento(@PathVariable String documento) {
        return ResponseEntity.ok(converterParaDTO(usuarioService.buscarPorDocumento(documento)));
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioRequestDTO request) {
        return ResponseEntity.ok(usuarioService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        usuarioService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Altera a senha de um usuário.
     *
     * @param id ID do usuário
     * @param senhaAtual senha atual para validação
     * @param novaSenha nova senha a ser definida
     * @return ResponseEntity<Void>
     * @throws RemessaException se a senha atual estiver incorreta
     */
    @PatchMapping("/{id}/senha")
    public ResponseEntity<Void> alterarSenha(
            @PathVariable Long id,
            @RequestParam String senhaAtual,
            @RequestParam String novaSenha) {
        usuarioService.alterarSenha(id, senhaAtual, novaSenha);
        return ResponseEntity.noContent().build();
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