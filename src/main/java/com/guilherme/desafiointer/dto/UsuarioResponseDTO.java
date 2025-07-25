package com.guilherme.desafiointer.dto;

import com.guilherme.desafiointer.domain.TipoUsuario;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class UsuarioResponseDTO {
    private Long id;
    private String nomeCompleto;
    private String email;
    private TipoUsuario tipoUsuario;
    private String documento;
    private BigDecimal saldo;
}