package com.guilherme.desafiointer.service.processor;

import com.guilherme.desafiointer.config.constants.AppConstants;
import com.guilherme.desafiointer.domain.*;
import com.guilherme.desafiointer.dto.remessa.RemessaRequestDTO;
import com.guilherme.desafiointer.exception.remessa.RemessaErrorType;
import com.guilherme.desafiointer.exception.remessa.RemessaException;
import com.guilherme.desafiointer.exception.domain.SaldoInsuficienteException;
import com.guilherme.desafiointer.repository.CarteiraRepository;
import com.guilherme.desafiointer.repository.RemessaRepository;
import com.guilherme.desafiointer.repository.TransacaoDiariaRepository;
import com.guilherme.desafiointer.service.interfaces.CotacaoServiceInterface;
import com.guilherme.desafiointer.service.strategy.StrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.cache.annotation.Cacheable;

/**
 * Implementação do processador de remessas internacionais.
 * Executa lógica de negócio transacional com controle de carteiras,
 * validação de limites, conversão de moedas e cache inteligente.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemessaProcessorImpl implements RemessaProcessor {

    private static final int SCALE_DIVISAO = 2;

    private final CotacaoServiceInterface cotacaoService;
    private final RemessaRepository remessaRepository;
    private final CarteiraRepository carteiraRepository;
    private final TransacaoDiariaRepository transacaoDiariaRepository;
    private final StrategyFactory strategyFactory;

    /**
     * Processa remessa completa com lock de carteiras e transação atômica.
     * Executa débito/crédito, validações e persistência em sequência segura.
     *
     * @param remessaRequestDTO dados da remessa
     * @return Remessa persistida com dados calculados
     */
    @Override
    public Remessa processarRemessa(RemessaRequestDTO remessaRequestDTO) {
        var dadosProcessamento = prepararDadosProcessamento(remessaRequestDTO);
        processarTransacao(dadosProcessamento);
        limparCachesAposTransacao();
        return criarEPersistirRemessa(remessaRequestDTO, dadosProcessamento);
    }

    /**
     * Busca histórico paginado com cache automático.
     * Chave de cache inclui usuário, período e página para precisão.
     *
     * @param usuario usuário alvo
     * @param inicio data inicial
     * @param fim data final
     * @param pageable configuração de paginação
     * @return página de remessas
     */
    @Override
    @Cacheable(
            cacheNames = AppConstants.CACHE_HISTORICO,
            key = "'hist_' + #usuario.id + '_' + #inicio.toString() + '_' + #fim.toString() + '_' + #pageable.pageNumber"
    )
    public Page<Remessa> buscarHistorico(Usuario usuario, LocalDateTime inicio,
                                         LocalDateTime fim, Pageable pageable) {
        return remessaRepository.buscarHistoricoTransacoes(usuario, inicio, fim, pageable);
    }

    /**
     * Record para dados calculados do processamento de remessa.
     * Encapsula carteiras, valores, taxas e cotações em estrutura imutável.
     */
    private record DadosProcessamentoRemessa(
            Carteira carteiraRemetente,
            Carteira carteiraDestinatario,
            TransacaoDiaria transacaoDiaria,
            BigDecimal taxa,
            BigDecimal valorTotalDebito,
            BigDecimal cotacao,
            BigDecimal valorConvertido,
            String moedaOrigem,      // 🆕 Parâmetro 8
            String moedaDestino     // 🆕 Parâmetro 9
    ) {}

    /**
     * Prepara todos os dados necessários para processamento da remessa.
     * Obtém carteiras com lock, calcula valores e valida operação.
     */
    private DadosProcessamentoRemessa prepararDadosProcessamento(RemessaRequestDTO remessaRequestDTO) {
        // Obter carteiras com lock pessimista
        Carteira carteiraRemetente = buscarCarteiraComLock(remessaRequestDTO.getUsuarioId());
        Carteira carteiraDestinatario = buscarCarteiraComLock(remessaRequestDTO.getDestinatarioId());

        // Determinar moedas de origem e destino
        String moedaDestino = remessaRequestDTO.getMoedaDestino().toUpperCase();
        String moedaOrigem = determinarMoedaOrigem(moedaDestino);

        log.debug("Preparando dados: moedaOrigem={}, moedaDestino={}, valor={}",
                moedaOrigem, moedaDestino, remessaRequestDTO.getValor());

        // Obter cotação
        BigDecimal cotacao = obterCotacao(moedaDestino);

        // Calcular taxa baseada no valor na moeda de origem
        BigDecimal taxa = strategyFactory.getTaxaStrategy(carteiraRemetente.getUsuario().getTipoUsuario())
                .calcularTaxa(remessaRequestDTO.getValor());

        // ✅ CORREÇÃO PRINCIPAL: Calcular valores baseado na conversão CORRETA
        BigDecimal valorConvertido;
        if ("USD".equalsIgnoreCase(moedaDestino)) {
            // BRL → USD: divide pela cotação (valor diminui)
            valorConvertido = remessaRequestDTO.getValor().divide(cotacao, 2, RoundingMode.HALF_UP);
            log.debug("Conversão BRL→USD: {} ÷ {} = {}", remessaRequestDTO.getValor(), cotacao, valorConvertido);
        } else if ("BRL".equalsIgnoreCase(moedaDestino)) {
            // USD → BRL: multiplica pela cotação (valor aumenta)
            valorConvertido = remessaRequestDTO.getValor().multiply(cotacao).setScale(2, RoundingMode.HALF_UP);
            log.debug("Conversão USD→BRL: {} × {} = {}", remessaRequestDTO.getValor(), cotacao, valorConvertido);
        } else {
            throw new IllegalArgumentException("Moeda de destino não suportada: " + moedaDestino);
        }

        BigDecimal valorTotalDebito = remessaRequestDTO.getValor().add(taxa);

        // Validar saldo na moeda de origem correta
        validarSaldo(carteiraRemetente, valorTotalDebito, moedaOrigem);

        // Processar limite diário
        TransacaoDiaria transacaoDiaria = processarLimiteDiario(carteiraRemetente, remessaRequestDTO.getValor());

        return new DadosProcessamentoRemessa(
                carteiraRemetente,
                carteiraDestinatario,
                transacaoDiaria,
                taxa,
                valorTotalDebito,
                cotacao,
                valorConvertido,
                moedaOrigem,
                moedaDestino
        );
    }

    /**
     * Determina a moeda de origem baseada na moeda de destino
     * Se destino é USD, origem é BRL e vice-versa
     */
    private String determinarMoedaOrigem(String moedaDestino) {
        if ("USD".equalsIgnoreCase(moedaDestino)) {
            return "BRL";
        } else if ("BRL".equalsIgnoreCase(moedaDestino)) {
            return "USD";
        } else {
            throw new IllegalArgumentException("Moeda de destino não suportada: " + moedaDestino);
        }
    }

    /**
     * Executa débito/crédito nas carteiras e persiste alterações.
     * Processa moedas origem/destino com valores corretos.
     */
    private void processarTransacao(DadosProcessamentoRemessa dados) {
        log.debug("Processando transação: moedaOrigem={}, moedaDestino={}, valor={}, valorConvertido={}",
                dados.moedaOrigem(), dados.moedaDestino(), dados.valorTotalDebito(), dados.valorConvertido());

        // Debitar da moeda de origem do remetente (valor + taxa)
        dados.carteiraRemetente().debitar(dados.valorTotalDebito(), dados.moedaOrigem());

        // Creditar na moeda de destino do destinatário (valor convertido)
        dados.carteiraDestinatario().creditar(dados.valorConvertido(), dados.moedaDestino());

        // Persistir alterações nas carteiras
        carteiraRepository.save(dados.carteiraRemetente());
        carteiraRepository.save(dados.carteiraDestinatario());

        // Atualizar a transação diária considerando o valor sem taxa
        atualizarTransacaoDiaria(dados.transacaoDiaria(),
                dados.valorTotalDebito().subtract(dados.taxa()));
    }

    /**
     * Valida e processa limite diário do usuario.
     * Utiliza strategy pattern para diferentes tipos de usuario.
     */
    private TransacaoDiaria processarLimiteDiario(Carteira carteira, BigDecimal valor) {
        var transacaoDiaria = buscarOuCriarTransacaoDiaria(carteira.getUsuario());
        strategyFactory.getLimiteValidator(carteira.getUsuario().getTipoUsuario())
                .validar(carteira.getUsuario(), transacaoDiaria.getValorTotal(), valor);
        return transacaoDiaria;
    }

    /**
     * Atualiza transação diária acumulando valor processado.
     * Persiste nova soma no repositório.
     */
    private void atualizarTransacaoDiaria(TransacaoDiaria transacaoDiaria, BigDecimal valor) {
        transacaoDiaria.atualizarValorTotal(transacaoDiaria.getValorTotal().add(valor));
        transacaoDiariaRepository.save(transacaoDiaria);
    }

    /**
     * Cria e persiste entidade Remessa com dados calculados.
     * Timestamp automático de criação.
     */
    private Remessa criarEPersistirRemessa(RemessaRequestDTO dto, DadosProcessamentoRemessa dados) {
        var remessa = Remessa.builder()
                .usuario(dados.carteiraRemetente().getUsuario())
                .destinatario(dados.carteiraDestinatario().getUsuario())
                .valor(dto.getValor())
                .valorConvertido(dados.valorConvertido())
                .taxa(dados.taxa())
                .moedaDestino(dto.getMoedaDestino())
                .cotacao(dados.cotacao())
                .dataCriacao(LocalDateTime.now())
                .build();

        return remessaRepository.save(remessa);
    }

    /**
     * Busca carteira com lock pessimista para evitar concorrência.
     * Lança RemessaException se carteira não encontrada.
     */
    private Carteira buscarCarteiraComLock(Long usuarioId) {
        return carteiraRepository.findByUsuarioIdWithPessimisticLock(usuarioId)
                .orElseThrow(() -> RemessaException.negocio(
                        RemessaErrorType.CARTEIRA_NAO_ENCONTRADA,
                        "Carteira não encontrada"
                ));
    }

    /**
     * Valida saldo suficiente na moeda especificada.
     * Lança SaldoInsuficienteException se inadequado.
     */
    private void validarSaldo(Carteira carteira, BigDecimal valor, String moeda) {
        BigDecimal saldoAtual;

        // Verifica o saldo correspondente à moeda especificada
        if ("BRL".equalsIgnoreCase(moeda)) {
            saldoAtual = carteira.getSaldoBRL();
        } else if ("USD".equalsIgnoreCase(moeda)) {
            saldoAtual = carteira.getSaldoUSD();
        } else {
            throw new IllegalArgumentException("Moeda não suportada: " + moeda);
        }

        // Valida se o saldo é suficiente para a operação
        if (saldoAtual.compareTo(valor) < 0) {
            throw new SaldoInsuficienteException(
                    String.format("Saldo insuficiente em %s para realizar a remessa", moeda)
            );
        }
    }

    /**
     * Busca ou cria transação diária com cache por usuário e data.
     * Cache evita consultas repetitivas no mesmo dia.
     *
     * @param usuario usuário da transação
     * @return TransacaoDiaria existente ou nova com valor zero
     */
    @Cacheable(
            cacheNames = AppConstants.CACHE_TOTAIS,
            key = "'total_' + #usuario.id + '_' + T(java.time.LocalDate).now()"
    )
    public TransacaoDiaria buscarOuCriarTransacaoDiaria(Usuario usuario) {
        return transacaoDiariaRepository.findByUsuarioAndData(usuario, LocalDate.now())
                .orElse(TransacaoDiaria.builder()
                        .usuario(usuario)
                        .data(LocalDate.now())
                        .valorTotal(BigDecimal.ZERO)
                        .build());
    }

    /**
     * Obtém cotação da moeda com cache por símbolo.
     * Cache por moeda evita chamadas excessivas à API do Banco Central.
     *
     * @param moedaDestino código da moeda (USD, BRL)
     * @return cotação válida ou exceção se inválida
     */
    @Cacheable(
            cacheNames = AppConstants.CACHE_COTACOES,
            key = "#moedaDestino",
            unless = "#result == null"
    )
    public BigDecimal obterCotacao(String moedaDestino) {
        BigDecimal cotacao = cotacaoService.obterCotacao(moedaDestino);
        if (cotacao == null || cotacao.compareTo(BigDecimal.ZERO) <= 0) {
            throw RemessaException.validacao(
                    RemessaErrorType.ERRO_COTACAO,
                    "Cotação inválida para a moeda: " + moedaDestino
            );
        }
        return cotacao;
    }

    /**
     * Limpa todos os caches após transação bem-sucedida.
     * Garante consistência após alterações em dados relacionados.
     */
    @CacheEvict(value = {
            AppConstants.CACHE_HISTORICO,
            AppConstants.CACHE_TOTAIS,
            AppConstants.CACHE_COTACOES},
            allEntries = true)
    public void limparCachesAposTransacao() {
    }
}