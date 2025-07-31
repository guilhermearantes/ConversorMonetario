
package com.guilherme.desafiointer.exception.base;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exceção base abstrata para erros de negócio da aplicação.
 *
 * Serve como classe pai para todas as exceções relacionadas a regras
 * de negócio, fornecendo estrutura padrão para tratamento de erros
 * e mapeamento de códigos HTTP.
 *
 * Características:
 * - Status HTTP padrão: 422 UNPROCESSABLE_ENTITY
 * - Suporte a causas raiz (chaining)
 * - Personalização de status por subclasse
 * - Integração com GlobalExceptionHandler
 */
@Getter
public abstract class BusinessException extends RuntimeException {

    /** Status HTTP associado ao erro de negócio */
    private final HttpStatus httpStatus;

    /**
     * Construtor padrão com status HTTP 422.
     *
     * @param message mensagem descritiva do erro
     */
    protected BusinessException(String message) {
        this(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * Construtor com status HTTP personalizado.
     *
     * @param message mensagem descritiva do erro
     * @param httpStatus código HTTP específico para o erro
     */
    protected BusinessException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    /**
     * Construtor completo com causa raiz e status personalizado.
     *
     * @param message mensagem descritiva do erro
     * @param cause exceção original que causou o erro
     * @param httpStatus código HTTP específico para o erro
     */
    protected BusinessException(String message, Throwable cause, HttpStatus httpStatus) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }
}