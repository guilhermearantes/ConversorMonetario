package com.guilherme.desafiointer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO para requisições de remessa internacional
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemessaDTO {
    @NotNull(message = "ID do usuário é obrigatório")
    private Long usuarioId;

    @NotNull(message = "ID do destinatário é obrigatório")
    private Long destinatarioId;

    @NotNull(message = "Valor é obrigatório")
    @Positive(message = "Valor deve ser maior que zero")
    private BigDecimal valor;

    @NotNull(message = "Moeda de destino é obrigatória")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Moeda destino deve seguir o padrão ISO-4217")
    private String moedaDestino;
}