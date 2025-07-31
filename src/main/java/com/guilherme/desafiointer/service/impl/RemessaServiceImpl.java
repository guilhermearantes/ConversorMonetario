package com.guilherme.desafiointer.service.impl;

import com.guilherme.desafiointer.config.constants.AppConstants;
import com.guilherme.desafiointer.domain.Remessa;
import com.guilherme.desafiointer.domain.Usuario;
import com.guilherme.desafiointer.dto.remessa.RemessaRequestDTO;
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
import java.util.function.Supplier;

/**
 * Implementação do serviço de remessas internacionais.
 *
 * Coordena transações de remessa com lock distribuído, cache inteligente
 * e tratamento diferenciado de exceções por categoria.
 */
@Service
@Validated
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheNames = AppConstants.CACHE_HISTORICO)
public class RemessaServiceImpl implements RemessaServiceInterface {

    private final RemessaProcessor remessaProcessor;
    private final RemessaValidator remessaValidator;

    /**
     * Realiza remessa internacional com transação ACID e lock distribuído.
     *
     * @param remessaRequestDTO dados da remessa
     * @return Remessa processada
     */
    @Override
    @Transactional
    public Remessa realizarRemessa(@Valid RemessaRequestDTO remessaRequestDTO) {
        return executarComLockDistribuido(
                remessaRequestDTO.getUsuarioId(),
                () -> processarRemessaSegura(remessaRequestDTO)
        );
    }

    /**
     * Processa remessa com logging estruturado e tratamento de exceções.
     * Categoriza erros em negócio, validação e processamento.
     */
    private Remessa processarRemessaSegura(RemessaRequestDTO remessaRequestDTO) {
        log.info("Iniciando processamento de remessa: [usuarioId={}, destinatarioId={}, valor={}, moeda={}]",
                remessaRequestDTO.getUsuarioId(),
                remessaRequestDTO.getDestinatarioId(),
                remessaRequestDTO.getValor(),
                remessaRequestDTO.getMoedaDestino());

        try {
            validarRemessa(remessaRequestDTO);
            Remessa remessa = processarRemessa(remessaRequestDTO);

            log.info("Remessa processada com sucesso: [id={}, usuarioId={}, valor={}, valorConvertido={}]",
                    remessa.getId(),
                    remessa.getUsuario().getId(),
                    remessa.getValor(),
                    remessa.getValorConvertido());

            return remessa;

        } catch (LimiteDiarioExcedidoException | SaldoInsuficienteException e) {
            log.warn("Erro de regra de negócio ao processar remessa: [tipo={}, usuarioId={}, valor={}] - {}",
                    e.getClass().getSimpleName(),
                    remessaRequestDTO.getUsuarioId(),
                    remessaRequestDTO.getValor(),
                    e.getMessage());
            throw e;

        } catch (RemessaException e) {
            log.error("Erro específico ao processar remessa: [tipo={}, usuarioId={}, errorType={}] - {}",
                    e.getClass().getSimpleName(),
                    remessaRequestDTO.getUsuarioId(),
                    e.getErrorType(),
                    e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("Erro inesperado ao processar remessa: [usuarioId={}, valor={}, moeda={}]",
                    remessaRequestDTO.getUsuarioId(),
                    remessaRequestDTO.getValor(),
                    remessaRequestDTO.getMoedaDestino(),
                    e);

            throw RemessaException.processamento(
                    RemessaErrorType.ERRO_PROCESSAMENTO,
                    "Erro ao processar remessa: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Executa operação com lock distribuído baseado no ID do usuario.
     * Previne transações concorrentes do mesmo usuario.
     */
    private <T> T executarComLockDistribuido(Long usuarioId, Supplier<T> operacao) {
        String lockKey = "remessa:usuario:" + usuarioId;
        try {
            if (!obterLockDistribuido(lockKey)) {
                throw RemessaException.negocio(
                        RemessaErrorType.OPERACAO_EM_ANDAMENTO,
                        "Já existe uma operação em andamento para este usuário"
                );
            }

            try {
                return operacao.get();
            } finally {
                liberarLockDistribuido(lockKey);
            }

        } catch (Exception e) {
            log.error("Erro ao executar operação com lock distribuído: [usuarioId={}]", usuarioId, e);
            throw e;
        }
    }

    /**
     * Obtém lock distribuído usando chave do usuario.
     * Implementação pendente para Redis/ZooKeeper.
     */
    private boolean obterLockDistribuido(String lockKey) {
        try {
            // Implementação do lock distribuído
            // Pode usar Redis, ZooKeeper, ou outra solução
            return true; // Implementar lógica real
        } catch (Exception e) {
            log.error("Erro ao obter lock distribuído: {}", lockKey, e);
            return false;
        }
    }

    /**
     * Libera lock distribuído de forma segura.
     * Trata erros de liberação sem propagar exceções.
     */
    private void liberarLockDistribuido(String lockKey) {
        try {
            // Implementação da liberação do lock distribuído
            log.debug("Lock distribuído liberado: {}", lockKey);
        } catch (Exception e) {
            log.error("Erro ao liberar lock distribuído: {}", lockKey, e);
        }
    }

    /**
     * Valida dados da remessa delegando para RemessaValidator.
     * Converte exceções genéricas para RemessaException tipadas.
     */
    private void validarRemessa(RemessaRequestDTO remessaRequestDTO) {
        try {
            remessaValidator.validarDadosRemessa(remessaRequestDTO);
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

    /**
     * Processa remessa delegando para RemessaProcessor.
     * Mantém exceções de negócio e converte erros inesperados.
     */
    private Remessa processarRemessa(RemessaRequestDTO remessaRequestDTO) {
        try {
            return remessaProcessor.processarRemessa(remessaRequestDTO);
        } catch (LimiteDiarioExcedidoException | SaldoInsuficienteException | RemessaException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao processar remessa: {}", remessaRequestDTO, e);
            throw RemessaException.processamento(
                    RemessaErrorType.ERRO_PROCESSAMENTO,
                    "Erro ao processar remessa: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Busca histórico paginado com cache automático.
     * Valida período, paginação e delega para processor.
     *
     * @param usuario usuário alvo
     * @param inicio data inicial
     * @param fim data final
     * @param pageable configuração de paginação
     * @return página de remessas
     */
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

    /**
     * Valida parâmetros de busca histórica.
     * Verifica período, paginação e delega validações específicas.
     */
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

    /**
     * Valida período de consulta histórica.
     * Verifica ordem das datas e limite máximo de dias.
     */
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

    /**
     * Valida parâmetros de paginação.
     * Verifica tamanho máximo de página configurado.
     */
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