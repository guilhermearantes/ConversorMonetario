package com.guilherme.desafiointer.exception.domain;

import com.guilherme.desafiointer.exception.base.BusinessException;

/**
 * Exceção lançada quando o usuário não possui saldo suficiente para a operação.
 * Esta exceção é disparada sempre que uma tentativa de débito exceder
 * o saldo disponível na moeda especificada (BRL ou USD).
 * Cenários de uso:
 * - Débito em carteira com saldo insuficiente
 * - Remessas com valor maior que saldo disponível
 * - Operações que incluem taxas excedendo limite
 * Características:
 * - Mensagens específicas por moeda (BRL/USD)
 * - Valores detalhados (saldo atual vs solicitado)
 * - Status HTTP: 422 UNPROCESSABLE_ENTITY (herdado)
 */

public class SaldoInsuficienteException extends BusinessException {

    /**
     * Construtor com mensagem descritiva do erro.
     *
     * @param message mensagem detalhando a insuficiência de saldo,
     *                incluindo moeda, saldo atual e valor solicitado
     */
    public SaldoInsuficienteException(String message) {
        super(message);
    }
}