package com.guilherme.desafiointer.remessa.exception;

public class RemessaException extends RuntimeException {
    public RemessaException(String message) {
        super(message);
    }

    public RemessaException(String message, Throwable cause) {
        super(message, cause);
    }
}