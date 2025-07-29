package com.guilherme.desafiointer.exception.domain;

import com.guilherme.desafiointer.exception.base.BusinessException;

/**
 * Exceção lançada quando o limite diário de transações é excedido.
 */
public class LimiteDiarioExcedidoException extends BusinessException {
    public LimiteDiarioExcedidoException(String message) {
        super(message);
    }
}