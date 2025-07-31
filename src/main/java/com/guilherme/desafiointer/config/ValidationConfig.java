package com.guilherme.desafiointer.config;

import com.guilherme.desafiointer.service.validator.DocumentoValidationService;
import com.guilherme.desafiointer.service.validator.DocumentoValidationHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração de validação de documentos da aplicação.
 *
 * Responsável por configurar e disponibilizar o serviço de validação
 * de CPF e CNPJ para uso em validações customizadas via Bean Validation.
 *
 * A classe utiliza o padrão Holder para disponibilizar o serviço de
 * validação em contextos onde injeção de dependência não é possível,
 * como em validadores customizados e enums.
 *
 * Fluxo de configuração:
 * 1. Cria instância do DocumentoValidationService
 * 2. Registra no DocumentoValidationHolder para acesso global
 * 3. Disponibiliza como bean Spring para injeção
 *
 * Usado por:
 * - TipoUsuario enum para validar documentos
 * - Validadores customizados de CPF/CNPJ
 */
@Configuration
public class ValidationConfig {

    /**
     * Cria e configura o serviço de validação de documentos.
     *
     * Além de criar o bean Spring, também registra a instância no
     * DocumentoValidationHolder para permitir acesso em contextos
     * onde injeção de dependência não é disponível.
     *
     * @return DocumentoValidationService configurado e registrado
     */
    @Bean
    public DocumentoValidationService documentoValidationService() {
        DocumentoValidationService service = new DocumentoValidationService();
        DocumentoValidationHolder.setService(service);
        return service;
    }
}