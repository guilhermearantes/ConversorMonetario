package com.guilherme.desafiointer.dto.remessa;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO para requisições de remessa internacional.
 *
 * Transporta dados de entrada do cliente para processamento de
 * transferências entre usuários PF/PJ com conversão automática BRL ↔ USD.
 *
 * Validações automáticas:
 * - IDs obrigatórios e não nulos
 * - Valor positivo e obrigatório
 * - Moeda no padrão ISO-4217 (3 letras maiúsculas)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemessaRequestDTO {

    /** ID do usuário remetente (origem da transferência) */
    @NotNull(message = "ID do usuário é obrigatório")
    private Long usuarioId;

    /** ID do usuário destinatário (destino da transferência) */
    @NotNull(message = "ID do destinatário é obrigatório")
    private Long destinatarioId;

    /** Valor a ser transferido na moeda de origem */
    @NotNull(message = "Valor é obrigatório")
    @Positive(message = "Valor deve ser maior que zero")
    private BigDecimal valor;

    /** Moeda de destino (USD, BRL) no padrão ISO-4217 */
    @NotNull(message = "Moeda de destino é obrigatória")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Moeda destino deve seguir o padrão ISO-4217")
    private String moedaDestino;
}