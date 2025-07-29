
package com.guilherme.desafiointer.service.interfaces;

import java.math.BigDecimal;

/**
 * Interface para serviço de cotação de moedas.
 * Define as operações disponíveis para obtenção de cotações.
 */
public interface CotacaoServiceInterface {
    /**
     * Obtém a cotação atual para a moeda especificada.
     *
     * @param moeda código da moeda (ex: "USD")
     * @return valor da cotação
     * @throws IllegalArgumentException se a moeda não for suportada
     */
    BigDecimal obterCotacao(String moeda);
}