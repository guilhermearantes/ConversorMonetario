package com.guilherme.desafiointer.exception.domain;

import com.guilherme.desafiointer.exception.base.BusinessException;

/**
 * Exceção lançada quando o limite diário de transações é excedido.
 * Esta exceção é disparada quando um usuário tenta realizar uma remessa
 * que faria com que o valor total de suas transações diárias exceda
 * o limite estabelecido para seu tipo de usuário.
 * Limites aplicados:
 * - Pessoa Física (PF): R$ 10.000,00 por dia
 * - Pessoa Jurídica (PJ): R$ 50.000,00 por dia
 * Status HTTP: 422 UNPROCESSABLE_ENTITY (herdado de BusinessException)
 */

public class LimiteDiarioExcedidoException extends BusinessException {

    /**
     * Construtor com mensagem descritiva do erro.
     *
     * @param message mensagem detalhando o limite excedido e tipo de usuário
     */
    public LimiteDiarioExcedidoException(String message) {
        super(message);
    }
}