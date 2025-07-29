package com.guilherme.desafiointer.service.validator;

import com.guilherme.desafiointer.domain.Usuario;
import com.guilherme.desafiointer.dto.RemessaDTO;
import com.guilherme.desafiointer.exception.remessa.RemessaErrorType;
import com.guilherme.desafiointer.exception.remessa.RemessaException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class RemessaValidator {


    public void validarDadosRemessa(RemessaDTO remessaDTO) {
        if (remessaDTO == null) {
            throw RemessaException.validacao(
                    RemessaErrorType.DADOS_INVALIDOS,
                    "RemessaDTO não pode ser nulo"
            );
        }

        if (remessaDTO.getUsuarioId() == null) {
            throw RemessaException.validacao(
                    RemessaErrorType.DADOS_INVALIDOS,
                    "ID do usuário remetente não pode ser nulo"
            );
        }

        if (remessaDTO.getDestinatarioId() == null) {
            throw RemessaException.validacao(
                    RemessaErrorType.DADOS_INVALIDOS,
                    "ID do usuário destinatário não pode ser nulo"
            );
        }

        if (remessaDTO.getValor() == null) {
            throw RemessaException.validacao(
                    RemessaErrorType.DADOS_INVALIDOS,
                    "Valor da remessa não pode ser nulo"
            );
        }

        if (remessaDTO.getValor().compareTo(BigDecimal.ZERO) <= 0) {
            throw RemessaException.validacao(
                    RemessaErrorType.DADOS_INVALIDOS,
                    "Valor da remessa deve ser maior que zero"
            );
        }

        if (remessaDTO.getMoedaDestino() == null || remessaDTO.getMoedaDestino().trim().isEmpty()) {
            throw RemessaException.validacao(
                    RemessaErrorType.MOEDA_NAO_SUPORTADA,
                    "Moeda de destino não pode ser nula ou vazia"
            );
        }

        if (remessaDTO.getUsuarioId().equals(remessaDTO.getDestinatarioId())) {
            throw RemessaException.validacao(
                    RemessaErrorType.DADOS_INVALIDOS,
                    "Usuário remetente e destinatário não podem ser o mesmo"
            );
        }
    }

    /**
     * Valida os parâmetros de busca do histórico de transações.
     *
     * @param usuario Usuário que está realizando a busca
     * @param inicio Data/hora inicial do período de busca
     * @param fim Data/hora final do período de busca
     * @param pageable Configuração de paginação
     * @throws IllegalArgumentException se algum parâmetro for inválido
     */
    public void validarParametrosBusca(Usuario usuario, LocalDateTime inicio,
                                       LocalDateTime fim, Pageable pageable) {
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não pode ser nulo");
        }

        if (inicio == null) {
            throw new IllegalArgumentException("Data inicial não pode ser nula");
        }

        if (fim == null) {
            throw new IllegalArgumentException("Data final não pode ser nula");
        }

        if (pageable == null) {
            throw new IllegalArgumentException("Configuração de paginação não pode ser nula");
        }

        if (inicio.isAfter(fim)) {
            throw new IllegalArgumentException("Data inicial não pode ser posterior à data final");
        }

        if (fim.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Data final não pode ser futura");
        }

        // Limita o período de busca a 90 dias
        if (inicio.plusDays(90).isBefore(fim)) {
            throw new IllegalArgumentException("Período de busca não pode ser superior a 90 dias");
        }

        validarPaginacao(pageable);
    }

    /**
     * Valida os parâmetros de paginação.
     *
     * @param pageable Configuração de paginação
     * @throws IllegalArgumentException se os parâmetros de paginação forem inválidos
     */
    private void validarPaginacao(Pageable pageable) {
        if (pageable.getPageSize() <= 0) {
            throw new IllegalArgumentException("Tamanho da página deve ser maior que zero");
        }

        if (pageable.getPageSize() > 100) {
            throw new IllegalArgumentException("Tamanho máximo da página é 100");
        }

        if (pageable.getPageNumber() < 0) {
            throw new IllegalArgumentException("Número da página deve ser maior ou igual a zero");
        }
    }
}