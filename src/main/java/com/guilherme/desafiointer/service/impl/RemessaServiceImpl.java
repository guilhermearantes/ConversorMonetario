package com.guilherme.desafiointer.service.impl;

import com.guilherme.desafiointer.config.constants.AppConstants;
import com.guilherme.desafiointer.domain.Remessa;
import com.guilherme.desafiointer.domain.Usuario;
import com.guilherme.desafiointer.dto.RemessaDTO;
import com.guilherme.desafiointer.exception.domain.LimiteDiarioExcedidoException;
import com.guilherme.desafiointer.exception.domain.SaldoInsuficienteException;
import com.guilherme.desafiointer.exception.remessa.RemessaErrorType;
import com.guilherme.desafiointer.exception.remessa.RemessaException;
import com.guilherme.desafiointer.service.interfaces.RemessaServiceInterface;
import com.guilherme.desafiointer.service.processor.RemessaProcessor;
import com.guilherme.desafiointer.service.validator.RemessaValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Implementação do serviço de remessas internacionais.
 */
@Service
@Validated
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheNames = AppConstants.CACHE_HISTORICO)
public class RemessaServiceImpl implements RemessaServiceInterface {

    private final RemessaProcessor remessaProcessor;
    private final RemessaValidator remessaValidator;

    @Override
    @Transactional
    public Remessa realizarRemessa(@Valid RemessaDTO remessaDTO) {
        log.info("Iniciando processamento de remessa: [usuarioId={}, valor={}, moeda={}]",
                remessaDTO.getUsuarioId(),
                remessaDTO.getValor(),
                remessaDTO.getMoedaDestino());

        validarRemessa(remessaDTO);

        try {
            Remessa remessa = processarRemessa(remessaDTO);
            log.info("Remessa processada com sucesso: [id={}]", remessa.getId());
            return remessa;
        } catch (LimiteDiarioExcedidoException | SaldoInsuficienteException | RemessaException e) {
            log.error("Erro de regra de negócio ao processar remessa: {}", e.getClass().getSimpleName());
            throw e;
        } catch (Exception e) {
            log.error("Erro no processamento da remessa: {}", e.getMessage(), e);
            throw RemessaException.processamento(
                    RemessaErrorType.ERRO_PROCESSAMENTO,
                    "Erro ao processar remessa",
                    e
            );
        }
    }

    private void validarRemessa(RemessaDTO remessaDTO) {
        try {
            remessaValidator.validarDadosRemessa(remessaDTO);
        } catch (RemessaException e) {
            // Propaga RemessaException diretamente
            throw e;
        } catch (IllegalArgumentException e) {
            // Converte IllegalArgumentException para DADOS_INVALIDOS
            throw RemessaException.validacao(
                    RemessaErrorType.DADOS_INVALIDOS,
                    e.getMessage()
            );
        } catch (Exception e) {
            // Outras exceções são convertidas para DADOS_INVALIDOS com mensagem específica
            throw RemessaException.validacao(
                    RemessaErrorType.DADOS_INVALIDOS,
                    "Erro na validação da remessa: " + e.getMessage()
            );
        }
    }

    private Remessa processarRemessa(RemessaDTO remessaDTO) {
        try {
            return remessaProcessor.processarRemessa(remessaDTO);
        } catch (LimiteDiarioExcedidoException | SaldoInsuficienteException e) {
            throw e;
        } catch (RemessaException e) {
            throw e;
        } catch (Exception e) {
            throw RemessaException.processamento(
                    RemessaErrorType.ERRO_PROCESSAMENTO,
                    "Erro ao processar remessa",
                    e
            );
        }
    }

    @Override
    @Cacheable(key = "#usuario.id + '_' + #inicio + '_' + #fim + '_' + #pageable.pageNumber")
    public Page<Remessa> buscarHistoricoTransacoes(
            Usuario usuario,
            LocalDateTime inicio,
            LocalDateTime fim,
            Pageable pageable) {

        log.debug("Buscando histórico de transações: [usuarioId={}, inicio={}, fim={}]",
                usuario.getId(), inicio, fim);

        try {
            validarParametrosBusca(usuario, inicio, fim, pageable);
            Page<Remessa> resultado = remessaProcessor.buscarHistorico(usuario, inicio, fim, pageable);

            log.debug("Histórico recuperado com sucesso: {} registros encontrados",
                    resultado.getTotalElements());

            return resultado;

        } catch (IllegalArgumentException e) {
            throw RemessaException.validacao(
                    RemessaErrorType.DADOS_INVALIDOS,
                    "Parâmetros inválidos para busca: " + e.getMessage()
            );
        } catch (Exception e) {
            throw RemessaException.processamento(
                    RemessaErrorType.ERRO_PROCESSAMENTO,
                    "Erro ao buscar histórico",
                    e
            );
        }
    }

    private void validarParametrosBusca(Usuario usuario, LocalDateTime inicio,
                                        LocalDateTime fim, Pageable pageable) {
        try {
            validarPeriodoConsulta(inicio, fim);
            validarPaginacao(pageable);
            remessaValidator.validarParametrosBusca(usuario, inicio, fim, pageable);
        } catch (Exception e) {
            throw RemessaException.validacao(
                    RemessaErrorType.DADOS_INVALIDOS,
                    "Erro na validação dos parâmetros de busca: " + e.getMessage()
            );
        }
    }

    private void validarPeriodoConsulta(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio.isAfter(fim)) {
            throw RemessaException.validacao(
                    RemessaErrorType.PERIODO_INVALIDO,
                    "Data inicial não pode ser posterior à data final"
            );
        }

        long diasPeriodo = ChronoUnit.DAYS.between(inicio, fim);
        if (diasPeriodo > AppConstants.PERIODO_MAXIMO_HISTORICO_DIAS) {
            throw RemessaException.validacao(
                    RemessaErrorType.PERIODO_INVALIDO,
                    String.format("Período máximo de consulta é de %d dias",
                            AppConstants.PERIODO_MAXIMO_HISTORICO_DIAS)
            );
        }
    }

    private void validarPaginacao(Pageable pageable) {
        if (pageable.getPageSize() > AppConstants.TAMANHO_MAXIMO_PAGINA) {
            throw RemessaException.validacao(
                    RemessaErrorType.PAGINACAO_INVALIDA,
                    String.format("Tamanho máximo de página é %d",
                            AppConstants.TAMANHO_MAXIMO_PAGINA)
            );
        }
    }
}