package com.guilherme.desafiointer.exception;

public class RemessaException extends RuntimeException {
    public RemessaException(String message) {
        super(message);
    }

    public RemessaException(String message, Throwable cause) {
        super(message, cause);
    }
}