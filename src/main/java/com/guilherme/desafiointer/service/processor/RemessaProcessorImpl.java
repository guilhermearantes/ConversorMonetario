package com.guilherme.desafiointer.service.processor;

import com.guilherme.desafiointer.config.constants.AppConstants;
import com.guilherme.desafiointer.domain.*;
import com.guilherme.desafiointer.dto.RemessaDTO;
import com.guilherme.desafiointer.exception.remessa.RemessaErrorType;
import com.guilherme.desafiointer.exception.remessa.RemessaException;
import com.guilherme.desafiointer.exception.domain.SaldoInsuficienteException;
import com.guilherme.desafiointer.repository.CarteiraRepository;
import com.guilherme.desafiointer.repository.RemessaRepository;
import com.guilherme.desafiointer.repository.TransacaoDiariaRepository;
import com.guilherme.desafiointer.service.interfaces.CotacaoServiceInterface;
import com.guilherme.desafiointer.service.strategy.StrategyFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.cache.annotation.Cacheable;

@Component
@RequiredArgsConstructor
public class RemessaProcessorImpl implements RemessaProcessor {

    private static final int SCALE_DIVISAO = 2;

    private final CotacaoServiceInterface cotacaoService;
    private final RemessaRepository remessaRepository;
    private final CarteiraRepository carteiraRepository;
    private final TransacaoDiariaRepository transacaoDiariaRepository;
    private final StrategyFactory strategyFactory;

    @Override
    public Remessa processarRemessa(RemessaDTO remessaDTO) {
        var dadosProcessamento = prepararDadosProcessamento(remessaDTO);
        processarTransacao(dadosProcessamento);
        limparCachesAposTransacao();
        return criarEPersistirRemessa(remessaDTO, dadosProcessamento);
    }

    @Override
    @Cacheable(
            cacheNames = AppConstants.CACHE_HISTORICO,
            key = "'hist_' + #usuario.id + '_' + #inicio.toString() + '_' + #fim.toString() + '_' + #pageable.pageNumber"
    )
    public Page<Remessa> buscarHistorico(Usuario usuario, LocalDateTime inicio,
                                         LocalDateTime fim, Pageable pageable) {
        return remessaRepository.buscarHistoricoTransacoes(usuario, inicio, fim, pageable);
    }

    private record DadosProcessamentoRemessa(
            Carteira carteiraRemetente,
            Carteira carteiraDestinatario,
            TransacaoDiaria transacaoDiaria,
            BigDecimal taxa,
            BigDecimal valorTotalDebito,
            BigDecimal cotacao,
            BigDecimal valorConvertido
    ) {}

    private DadosProcessamentoRemessa prepararDadosProcessamento(RemessaDTO remessaDTO) {
        var carteiraRemetente = buscarCarteiraComLock(remessaDTO.getUsuarioId());
        var carteiraDestinatario = buscarCarteiraComLock(remessaDTO.getDestinatarioId());
        var transacaoDiaria = processarLimiteDiario(carteiraRemetente, remessaDTO.getValor());
        var taxa = calcularTaxa(carteiraRemetente, remessaDTO.getValor());
        var valorTotalDebito = remessaDTO.getValor().add(taxa);

        validarSaldo(carteiraRemetente, valorTotalDebito);

        var cotacao = obterCotacao(remessaDTO.getMoedaDestino());
        var valorConvertido = calcularValorConvertido(remessaDTO.getValor(), cotacao);

        return new DadosProcessamentoRemessa(
                carteiraRemetente, carteiraDestinatario, transacaoDiaria,
                taxa, valorTotalDebito, cotacao, valorConvertido);
    }

    private void processarTransacao(DadosProcessamentoRemessa dados) {
        dados.carteiraRemetente().debitar(dados.valorTotalDebito());
        dados.carteiraDestinatario().creditar(dados.valorConvertido());

        carteiraRepository.save(dados.carteiraRemetente());
        carteiraRepository.save(dados.carteiraDestinatario());

        atualizarTransacaoDiaria(dados.transacaoDiaria(),
                dados.valorTotalDebito().subtract(dados.taxa()));
    }

    private TransacaoDiaria processarLimiteDiario(Carteira carteira, BigDecimal valor) {
        var transacaoDiaria = buscarOuCriarTransacaoDiaria(carteira.getUsuario());
        strategyFactory.getLimiteValidator(carteira.getUsuario().getTipoUsuario())
                .validar(carteira.getUsuario(), transacaoDiaria.getValorTotal(), valor);
        return transacaoDiaria;
    }

    private BigDecimal calcularTaxa(Carteira carteira, BigDecimal valor) {
        return strategyFactory.getTaxaStrategy(carteira.getUsuario().getTipoUsuario())
                .calcularTaxa(valor);
    }

    private BigDecimal calcularValorConvertido(BigDecimal valor, BigDecimal cotacao) {
        return valor.divide(cotacao, SCALE_DIVISAO, RoundingMode.HALF_UP);
    }

    private void atualizarTransacaoDiaria(TransacaoDiaria transacaoDiaria, BigDecimal valor) {
        transacaoDiaria.atualizarValorTotal(transacaoDiaria.getValorTotal().add(valor));
        transacaoDiariaRepository.save(transacaoDiaria);
    }

    private Remessa criarEPersistirRemessa(RemessaDTO dto, DadosProcessamentoRemessa dados) {
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

    private Carteira buscarCarteiraComLock(Long usuarioId) {
        return carteiraRepository.findByUsuarioIdWithPessimisticLock(usuarioId)
                .orElseThrow(() -> RemessaException.negocio(
                        RemessaErrorType.CARTEIRA_NAO_ENCONTRADA,
                        "Carteira não encontrada"
                ));
    }

    private void validarSaldo(Carteira carteira, BigDecimal valor) {
        if (carteira.getSaldo().compareTo(valor) < 0) {
            throw new SaldoInsuficienteException("Saldo insuficiente para realizar a remessa");
        }
    }

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

    @Cacheable(
            cacheNames = AppConstants.CACHE_COTACOES,
            key = "#moedaDestino"
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

    @CacheEvict(value = {
            AppConstants.CACHE_HISTORICO,
            AppConstants.CACHE_TOTAIS,
            AppConstants.CACHE_COTACOES},
            allEntries = true)
    public void limparCachesAposTransacao() {
    }
}