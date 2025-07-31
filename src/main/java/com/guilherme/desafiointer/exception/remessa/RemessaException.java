package com.guilherme.desafiointer.exception.remessa;

import com.guilherme.desafiointer.exception.base.BusinessException;
import lombok.Getter;

/**
 * Exceção unificada para erros relacionados a remessas.
 *
 * Utiliza o padrão Factory Method para criar exceções específicas
 * baseadas em tipos de erro predefinidos (RemessaErrorType).
 *
 * Características:
 * - Tipagem forte via enum RemessaErrorType
 * - Factory methods por categoria de erro
 * - Detalhes opcionais para contexto adicional
 * - Status HTTP automático baseado no tipo
 * - Suporte a causa raiz (exception chaining)
 *
 * Categorias de erro:
 * - validacao(): Erros de entrada/dados inválidos
 * - negocio(): Violações de regras de negócio
 * - processamento(): Erros técnicos/infraestrutura
 *
 * Integração:
 * - GlobalExceptionHandler: Tratamento especializado
 * - RemessaErrorType: Mapeamento para HTTP status
 * - BusinessException: Herança de comportamento base
 */
@Getter
public class RemessaException extends BusinessException {

    /** Tipo específico do erro baseado em enum */
    private final RemessaErrorType errorType;

    /** Detalhes opcionais para contexto adicional */
    private final String detail;

    /**
     * Construtor privado para mensagem simples.
     *
     * @param errorType tipo do erro definido no enum
     * @param detail detalhes opcionais (pode ser null)
     */
    private RemessaException(RemessaErrorType errorType, String detail) {
        super(errorType.getMessage() + (detail != null ? ": " + detail : ""), errorType.getHttpStatus());
        this.errorType = errorType;
        this.detail = detail;
    }

    /**
     * Construtor privado para mensagem com causa raiz.
     *
     * @param errorType tipo do erro definido no enum
     * @param detail detalhes opcionais (pode ser null)
     * @param cause exceção original que causou o erro
     */
    private RemessaException(RemessaErrorType errorType, String detail, Throwable cause) {
        super(errorType.getMessage() + (detail != null ? ": " + detail : ""), cause, errorType.getHttpStatus());
        this.errorType = errorType;
        this.detail = detail;
    }

    /**
     * Cria exceção de validação com detalhes.
     *
     * @param errorType tipo do erro de validação
     * @param detail contexto adicional do erro
     * @return RemessaException configurada
     */
    public static RemessaException validacao(RemessaErrorType errorType, String detail) {
        return new RemessaException(errorType, detail);
    }

    /**
     * Cria exceção de negócio com detalhes.
     *
     * @param errorType tipo do erro de negócio
     * @param detail contexto adicional do erro
     * @return RemessaException configurada
     */
    public static RemessaException negocio(RemessaErrorType errorType, String detail) {
        return new RemessaException(errorType, detail);
    }

    /**
     * Cria exceção de processamento com detalhes e causa.
     *
     * @param errorType tipo do erro de processamento
     * @param detail contexto adicional do erro
     * @param cause exceção original
     * @return RemessaException configurada
     */
    public static RemessaException processamento(RemessaErrorType errorType, String detail, Throwable cause) {
        return new RemessaException(errorType, detail, cause);
    }
}