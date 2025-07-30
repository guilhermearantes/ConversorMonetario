package com.guilherme.desafiointer.dto.remessa;


import com.guilherme.desafiointer.domain.Remessa;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para respostas de remessa internacional
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemessaResponseDTO {
    private Long id;
    private Long usuarioId;
    private Long destinatarioId;
    private BigDecimal valor;
    private BigDecimal valorConvertido;
    private BigDecimal taxa;
    private String moedaDestino;
    private BigDecimal cotacao;
    private LocalDateTime dataCriacao;

    public static RemessaResponseDTO from(Remessa remessa) {
        return RemessaResponseDTO.builder()
                .id(remessa.getId())
                .usuarioId(remessa.getUsuario().getId())
                .destinatarioId(remessa.getDestinatario().getId())
                .valor(remessa.getValor())
                .valorConvertido(remessa.getValorConvertido())
                .taxa(remessa.getTaxa())
                .moedaDestino(remessa.getMoedaDestino())
                .cotacao(remessa.getCotacao())
                .dataCriacao(remessa.getDataCriacao())
                .build();
    }
}