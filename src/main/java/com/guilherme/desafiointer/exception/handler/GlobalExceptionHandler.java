package com.guilherme.desafiointer.exception.handler;

import com.guilherme.desafiointer.dto.error.ErrorResponse;
import com.guilherme.desafiointer.exception.base.BusinessException;
import com.guilherme.desafiointer.exception.domain.LimiteDiarioExcedidoException;
import com.guilherme.desafiointer.exception.domain.SaldoInsuficienteException;
import com.guilherme.desafiointer.exception.remessa.RemessaException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manipulador global de exceções para toda a aplicação.
 *
 * Intercepta e processa todas as exceções lançadas pelos controllers,
 * convertendo-as em responses HTTP estruturados e padronizados.
 *
 * Características:
 * - Tratamento centralizado via @RestControllerAdvice
 * - Mapeamento específico por tipo de exceção
 * - Logging diferenciado por criticidade
 * - Responses padronizados com ErrorResponse
 * - Suporte a validações Spring Boot
 *
 * Hierarquia de tratamento:
 * 1. RemessaException (mais específica)
 * 2. BusinessException (intermediária)
 * 3. MethodArgumentNotValidException (validação)
 * 4. Exception (fallback geral)
 *
 * Status HTTP mapeados:
 * - RemessaException: Status definido por RemessaErrorType
 * - BusinessException: 422 UNPROCESSABLE_ENTITY (padrão)
 * - Validation: 400 BAD_REQUEST
 * - Exception: 500 INTERNAL_SERVER_ERROR
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Mapa de status HTTP específicos para subclasses de BusinessException.
     * Permite override do status padrão 422 para casos particulares.
     */
    private static final Map<Class<? extends BusinessException>, HttpStatus> EXCEPTION_STATUS_MAP = Map.of(
            SaldoInsuficienteException.class, HttpStatus.UNPROCESSABLE_ENTITY,
            LimiteDiarioExcedidoException.class, HttpStatus.UNPROCESSABLE_ENTITY
    );

    /**
     * Trata exceções específicas de remessas com detalhes customizados.
     *
     * @param ex RemessaException capturada
     * @param request requisição HTTP atual
     * @return ResponseEntity com ErrorResponse detalhado
     */
    @ExceptionHandler(RemessaException.class)
    public ResponseEntity<ErrorResponse> handleRemessaException(
            RemessaException ex, HttpServletRequest request) {
        log.error("Erro em remessa: {} - Tipo: {} - Detalhe: {}",
                ex.getMessage(),
                ex.getErrorType(),
                ex.getDetail());

        Map<String, String> details = null;
        if (ex.getDetail() != null) {
            details = Map.of("detail", ex.getDetail());
        }

        return createErrorResponse(
                ex.getMessage(),
                ex.getErrorType().getHttpStatus(),
                request.getRequestURI(),
                details
        );
    }

    /**
     * Trata exceções gerais de negócio (fallback para BusinessException).
     *
     * @param ex BusinessException ou subclasse
     * @param request requisição HTTP atual
     * @return ResponseEntity com ErrorResponse padrão
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        log.error("Erro de negócio: {}", ex.getMessage());
        HttpStatus status = determineHttpStatus(ex);
        return createErrorResponse(
                ex.getMessage(),
                status,
                request.getRequestURI(),
                null
        );
    }

    /**
     * Trata erros de validação do Spring Boot (@Valid).
     *
     * @param ex MethodArgumentNotValidException do Spring
     * @param request requisição HTTP atual
     * @return ResponseEntity com detalhes dos campos inválidos
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null ?
                                fieldError.getDefaultMessage() : "Erro de validação",
                        (error1, error2) -> error1
                ));

        log.debug("Erros de validação: {}", errors);

        return createErrorResponse(
                "Erro de validação dos dados",
                HttpStatus.BAD_REQUEST,
                request.getRequestURI(),
                errors
        );
    }

    /**
     * Fallback para exceções não tratadas especificamente.
     *
     * @param ex Exception genérica
     * @param request requisição HTTP atual
     * @return ResponseEntity com erro genérico 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedExceptions(
            Exception ex, HttpServletRequest request) {
        log.error("Erro não esperado: ", ex);
        return createErrorResponse(
                "Erro interno do servidor",
                HttpStatus.INTERNAL_SERVER_ERROR,
                request.getRequestURI(),
                null
        );
    }

    /**
     * Determina o status HTTP baseado no tipo específico de BusinessException.
     *
     * @param ex BusinessException a ser mapeada
     * @return HttpStatus correspondente ou INTERNAL_SERVER_ERROR como fallback
     */
    private HttpStatus determineHttpStatus(BusinessException ex) {
        return EXCEPTION_STATUS_MAP.getOrDefault(ex.getClass(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Cria um ResponseEntity padronizado com ErrorResponse.
     *
     * @param message mensagem principal do erro
     * @param status código HTTP
     * @param path URI da requisição que gerou o erro
     * @param errors detalhes adicionais (opcional)
     * @return ResponseEntity formatado
     */
    private ResponseEntity<ErrorResponse> createErrorResponse(
            String message,
            HttpStatus status,
            String path,
            Map<String, String> errors) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .message(message)
                .path(path)
                .errors(errors)
                .build();

        return new ResponseEntity<>(errorResponse, status);
    }
}