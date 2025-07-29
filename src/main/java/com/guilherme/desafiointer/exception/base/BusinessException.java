
package com.guilherme.desafiointer.exception.base;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exceção base para erros de negócio da aplicação.
 */
@Getter
public abstract class BusinessException extends RuntimeException {
    private final HttpStatus httpStatus;

    protected BusinessException(String message) {
        this(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    protected BusinessException(String message, Throwable cause) {
        this(message, cause, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    protected BusinessException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    protected BusinessException(String message, Throwable cause, HttpStatus httpStatus) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }
}