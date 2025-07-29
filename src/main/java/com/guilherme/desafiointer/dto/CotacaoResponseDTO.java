package com.guilherme.desafiointer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para resposta de cotações de moeda
 */
@Getter
@AllArgsConstructor
public class CotacaoResponseDTO {
    @NotNull(message = "Valor da cotação não pode ser nulo")
    private BigDecimal valor;

    @NotNull(message = "Data/hora da cotação não pode ser nula")
    private LocalDateTime dataHora;

    @NotNull(message = "Moeda não pode ser nula")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Moeda deve seguir o padrão ISO-4217")
    private String moeda;
}