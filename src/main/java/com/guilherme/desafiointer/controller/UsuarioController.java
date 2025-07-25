package com.guilherme.desafiointer.controller;
import com.guilherme.desafiointer.domain.Usuario;
import com.guilherme.desafiointer.dto.UsuarioRequestDTO;
import com.guilherme.desafiointer.dto.UsuarioResponseDTO;
import com.guilherme.desafiointer.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

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