package com.guilherme.desafiointer.exception;

public class LimiteDiarioExcedidoException extends RuntimeException {
    public LimiteDiarioExcedidoException(String message) {
        super(message);
    }
}
