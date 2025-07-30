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

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Map<Class<? extends BusinessException>, HttpStatus> EXCEPTION_STATUS_MAP = Map.of(
            SaldoInsuficienteException.class, HttpStatus.UNPROCESSABLE_ENTITY,
            LimiteDiarioExcedidoException.class, HttpStatus.UNPROCESSABLE_ENTITY
    );

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

    private HttpStatus determineHttpStatus(BusinessException ex) {
        return EXCEPTION_STATUS_MAP.getOrDefault(ex.getClass(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

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