package com.guilherme.desafiointer.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;

/**
 * DTO para status do serviço de cotações
 */
@Getter
@Builder
@AllArgsConstructor
public class CotacaoStatusDTO {
    @NotNull(message = "Status do serviço não pode ser nulo")
    private boolean online;

    @NotNull(message = "Mensagem de status não pode ser nula")
    private String mensagem;

    private BigDecimal ultimaCotacao; // Pode ser nulo em caso de erro
}