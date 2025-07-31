package com.guilherme.desafiointer.domain;

import com.guilherme.desafiointer.repository.CotacaoHistoricoRepository;
import com.guilherme.desafiointer.service.impl.CotacaoServiceImpl;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade que armazena o histórico de cotações de moedas.
 *
 * Mantém registro completo de todas as cotações obtidas da API do Banco Central,
 * permitindo fallback para cotações anteriores quando a API está indisponível
 * ou em finais de semana/feriados quando não há cotação oficial.
 *
 * Funcionalidades principais:
 * - Armazenamento de cotações com timestamp preciso
 * - Distinção entre cotações de dias úteis e fins de semana
 * - Consulta da última cotação útil disponível
 * - Histórico completo para auditoria e análise
 *
 * Regras de negócio:
 * - Cotações de fins de semana são marcadas como isFimDeSemana=true
 * - Sistema sempre tenta usar última cotação útil (dia útil) como fallback
 * - Precisão de 4 casas decimais para valores de cotação
 * - Timestamp de criação e última atualização para auditoria
 *
 * Usado por:
 * - CotacaoService para fallback em caso de indisponibilidade da API
 * - Consultas de cotações históricas para análise
 * - Garantia de continuidade do serviço em finais de semana
 *
 * @see CotacaoServiceImpl
 * @see CotacaoHistoricoRepository
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Table(name = "cotacoes_historico")
public class CotacaoHistorico {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Código da moeda (ex: "USD", "EUR").
     * Referência para identificar a moeda da cotação.
     */
    @Column(nullable = false)
    private String moeda;

    /**
     * Valor da cotação com precisão de 4 casas decimais.
     * Representa quantas unidades da moeda base (BRL) equivalem
     * a uma unidade da moeda estrangeira.
     */
    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal valor;

    /**
     * Data e hora exata da cotação.
     * Timestamp da cotação oficial ou do momento da consulta.
     */
    @Column(nullable = false)
    private LocalDateTime dataHora;

    /**
     * Indica se a cotação foi obtida em final de semana.
     *
     * true = Cotação de final de semana (reutilizada da última útil)
     * false = Cotação de dia útil (obtida da API oficial)
     */
    @Column(nullable = false)
    private boolean isFimDeSemana;

    /**
     * Timestamp da última atualização do registro.
     * Para auditoria e controle de versão dos dados.
     */
    @Column(nullable = false)
    private LocalDateTime ultimaAtualizacao;
}