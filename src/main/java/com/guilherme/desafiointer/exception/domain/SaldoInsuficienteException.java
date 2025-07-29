package com.guilherme.desafiointer.exception.domain;

import com.guilherme.desafiointer.exception.base.BusinessException;

/**
 * Exceção lançada quando o usuário não possui saldo suficiente para a operação.
 */
public class SaldoInsuficienteException extends BusinessException {
    public SaldoInsuficienteException(String message) {
        super(message);
    }
}