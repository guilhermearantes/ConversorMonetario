package com.guilherme.desafiointer.dto.integration.bcb;

import lombok.Data;
import java.util.List;

/**
 * DTO para resposta da API PTAX do Banco Central do Brasil.
 *
 * Utilizado para deserializar cotações USD/BRL obtidas da API oficial
 * do BCB, processando dados em tempo real para remessas internacionais.
 *
 * Endpoint: api.bcb.gov.br/dados/serie/bcdata.sgs.1/dados
 */
@Data
public class PTAXResponse {

    /** Lista de cotações retornadas pela API */
    private List<PTAXValue> value;

    /**
     * Classe interna que representa uma cotação individual.
     * Contém valores de compra/venda e timestamp da cotação oficial.
     */
    @Data
    public static class PTAXValue {

        /** Valor de cotação para compra (utilizado nas remessas) */
        private String cotacaoCompra;

        /** Valor de cotação para venda */
        private String cotacaoVenda;

        /** Data e hora da cotação no formato BCB */
        private String dataHoraCotacao;
    }
}