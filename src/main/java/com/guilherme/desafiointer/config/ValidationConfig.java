package com.guilherme.desafiointer.config;

import com.guilherme.desafiointer.service.validator.DocumentoValidationService;
import com.guilherme.desafiointer.service.validator.DocumentoValidationHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ValidationConfig {

    @Bean
    public DocumentoValidationService documentoValidationService() {
        DocumentoValidationService service = new DocumentoValidationService();
        DocumentoValidationHolder.setService(service);
        return service;
    }
}