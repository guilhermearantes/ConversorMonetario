package com.guilherme.desafiointer.exception.remessa;

import com.guilherme.desafiointer.exception.base.BusinessException;
import lombok.Getter;

/**
 * Exceção unificada para erros relacionados a remessas.
 */
@Getter
public class RemessaException extends BusinessException {

    private final RemessaErrorType errorType;
    private final String detail;

    private RemessaException(RemessaErrorType errorType, String detail) {
        super(errorType.getMessage() + (detail != null ? ": " + detail : ""), errorType.getHttpStatus());
        this.errorType = errorType;
        this.detail = detail;
    }

    private RemessaException(RemessaErrorType errorType, String detail, Throwable cause) {
        super(errorType.getMessage() + (detail != null ? ": " + detail : ""), cause, errorType.getHttpStatus());
        this.errorType = errorType;
        this.detail = detail;
    }

    public static RemessaException validacao(RemessaErrorType errorType) {
        return new RemessaException(errorType, null);
    }

    public static RemessaException validacao(RemessaErrorType errorType, String detail) {
        return new RemessaException(errorType, detail);
    }

    public static RemessaException negocio(RemessaErrorType errorType) {
        return new RemessaException(errorType, null);
    }

    public static RemessaException negocio(RemessaErrorType errorType, String detail) {
        return new RemessaException(errorType, detail);
    }

    public static RemessaException processamento(RemessaErrorType errorType, Throwable cause) {
        return new RemessaException(errorType, null, cause);
    }

    public static RemessaException processamento(RemessaErrorType errorType, String detail, Throwable cause) {
        return new RemessaException(errorType, detail, cause);
    }
}