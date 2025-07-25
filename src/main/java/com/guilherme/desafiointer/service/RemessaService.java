package com.guilherme.desafiointer.service;

import com.guilherme.desafiointer.domain.Carteira;
import com.guilherme.desafiointer.domain.Remessa;
import com.guilherme.desafiointer.domain.TransacaoDiaria;
import com.guilherme.desafiointer.domain.Usuario;
import com.guilherme.desafiointer.exception.CarteiraNotFoundException;
import com.guilherme.desafiointer.exception.LimiteDiarioExcedidoException;
import com.guilherme.desafiointer.exception.SaldoInsuficienteException;
import com.guilherme.desafiointer.dto.RemessaDTO;
import com.guilherme.desafiointer.repository.CarteiraRepository;
import com.guilherme.desafiointer.repository.RemessaRepository;
import com.guilherme.desafiointer.repository.TransacaoDiariaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RemessaService {

    private final RemessaRepository remessaRepository;
    private final CarteiraRepository carteiraRepository;
    private final TransacaoDiariaRepository transacaoDiariaRepository;
    private final CotacaoService cotacaoService;

    @Transactional
    public Remessa realizarRemessa(RemessaDTO remessaDTO) {
        Carteira carteira = carteiraRepository.findByUsuarioId(remessaDTO.getUsuarioId())
                .orElseThrow(() -> new EntityNotFoundException("Carteira não encontrada para o usuário informado"));

        validarSaldoSuficiente(carteira, remessaDTO.getValor());
        validarLimitesDiarios(carteira.getUsuario(), remessaDTO.getValor());

        BigDecimal taxa = calcularTaxa(carteira.getUsuario(), remessaDTO.getValor());
        BigDecimal valorTotal = remessaDTO.getValor().add(taxa);
        BigDecimal cotacao = cotacaoService.obterCotacao(remessaDTO.getMoedaDestino());

        carteira.debitar(valorTotal);
        carteiraRepository.save(carteira);

        Remessa remessa = Remessa.builder()
                .usuario(carteira.getUsuario())
                .valor(remessaDTO.getValor())
                .taxa(taxa)
                .moedaDestino(remessaDTO.getMoedaDestino())
                .cotacao(cotacao)
                .dataCriacao(LocalDateTime.now())
                .build();

        atualizarTransacaoDiaria(carteira.getUsuario(), remessaDTO.getValor());

        return remessaRepository.save(remessa);
    }

    private void validarSaldoSuficiente(Carteira carteira, BigDecimal valor) {
        validarSaldo(carteira, valor);
    }

    private void validarLimitesDiarios(Usuario usuario, BigDecimal valor) {
        validarLimiteDiario(usuario, valor);
    }

    private Carteira buscarCarteiraComLock(Usuario usuario) {
        return carteiraRepository.findByUsuarioIdWithLock(usuario.getId())
                .orElseThrow(() -> new CarteiraNotFoundException(
                        "Carteira não encontrada para o usuário: " + usuario.getId()));
    }

    private void validarSaldo(Carteira carteira, BigDecimal valor) {
        if (carteira.getSaldo().compareTo(valor) < 0) {
            throw new SaldoInsuficienteException("Saldo insuficiente para realizar a remessa");
        }
    }

    private void validarLimiteDiario(Usuario usuario, BigDecimal novoValor) {
        var transacaoDiaria = buscarOuCriarTransacaoDiaria(usuario);
        var novoTotal = transacaoDiaria.getValorTotal().add(novoValor);

        if (novoTotal.compareTo(usuario.getTipoUsuario().getLimiteDiario()) > 0) {
            throw new LimiteDiarioExcedidoException("Limite diário de transações excedido");
        }
    }

    private TransacaoDiaria buscarOuCriarTransacaoDiaria(Usuario usuario) {
        var hoje = LocalDate.now();
        return transacaoDiariaRepository.findByUsuarioAndData(usuario, hoje)
                .orElse(TransacaoDiaria.builder()
                        .usuario(usuario)
                        .data(hoje)
                        .valorTotal(BigDecimal.ZERO)
                        .build());
    }

    private BigDecimal calcularTaxa(Usuario usuario, BigDecimal valor) {
        return usuario.getTipoUsuario().calcularTaxa(valor)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void atualizarTransacaoDiaria(Usuario usuario, BigDecimal valor) {
        var transacaoDiaria = buscarOuCriarTransacaoDiaria(usuario);
        transacaoDiaria.atualizarValorTotal(transacaoDiaria.getValorTotal().add(valor));
        transacaoDiariaRepository.save(transacaoDiaria);
    }

    public Page<Remessa> buscarHistoricoTransacoes(Usuario usuario,
                                                   LocalDateTime inicio,
                                                   LocalDateTime fim,
                                                   Pageable pageable) {
        return remessaRepository.buscarHistoricoTransacoes(usuario, inicio, fim, pageable);
    }

    public BigDecimal calcularTotalEnviado(Usuario usuario, LocalDateTime inicio, LocalDateTime fim) {
        return remessaRepository.calcularTotalEnviadoPorPeriodo(usuario, inicio, fim);
    }

    public BigDecimal calcularTotalTaxas(Usuario usuario, LocalDateTime inicio, LocalDateTime fim) {
        return remessaRepository.calcularTotalTaxasPorPeriodo(usuario, inicio, fim);
    }
}