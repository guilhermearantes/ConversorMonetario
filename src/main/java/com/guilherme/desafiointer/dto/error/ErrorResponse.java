package com.guilherme.desafiointer.dto.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO para padronização das respostas de erro da API.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private final LocalDateTime timestamp;
    private final int status;
    private final String message;
    private final String path;
    private final Map<String, String> errors;
}