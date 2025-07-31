package com.guilherme.desafiointer.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade que representa uma remessa internacional entre usuarios.
 *
 * Mantém registro completo de transferências internacionais realizadas entre
 * usuarios PF/PJ, incluindo dados de conversão de moedas, taxas aplicadas
 * e informações de auditoria da transação.
 *
 * Funcionalidades principais:
 * - Registro de remessas BRL → USD com cotação oficial do Banco Central
 * - Controle de taxas diferenciadas por tipo de usuario (PF/PJ)
 * - Rastreabilidade completa com timestamps de criação
 * - Suporte a consultas históricas por período
 *
 * Regras de negócio implementadas:
 * - Valor mínimo e máximo por transação conforme regulamentação
 * - Aplicação de taxas: 2% PF, 1% PJ sobre valor + cotação
 * - Conversão automática BRL→USD usando cotação do BC
 * - Validação de saldo suficiente antes da transferência
 * - Controle de limites diários: R$ 10.000 PF, R$ 50.000 PJ
 *
 * Relacionamentos:
 * - ManyToOne com Usuario (remetente) - obrigatório
 * - ManyToOne com Usuario (destinatário) - obrigatório
 *
 * Campos calculados:
 * - valorConvertido: valor final em USD após aplicação da cotação
 * - taxa: valor da taxa cobrada em BRL conforme tipo de usuario
 *
 * Utilizada por:
 * - RemessaService para processamento de transferências
 * - RemessaRepository para consultas históricas e totalizações
 * - Relatórios de compliance e auditoria
 *
 * @see com.guilherme.desafiointer.service.interfaces.RemessaServiceInterface
 * @see com.guilherme.desafiointer.repository.RemessaRepository
 * @see com.guilherme.desafiointer.domain.Usuario
 * @see com.guilherme.desafiointer.domain.TransacaoDiaria
 */
@Entity
@Table(name = "remessas")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Remessa {

    /**
     * Identificador único da remessa.
     * Chave primária gerada pelo banco de dados.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Usuario remetente da transação.
     *
     * Referência para o utilizador que esta enviando o valor.
     * Relacionamento obrigatório com fetch LAZY para performance.
     *
     * @see com.guilherme.desafiointer.domain.Usuario
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /**
     * Usuário destinatário da transação.
     *
     * Referência para o usuário que receberá o valor convertido.
     * Relacionamento obrigatório com fetch LAZY para performance.
     *
     * @see com.guilherme.desafiointer.domain.Usuario
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinatario_id", nullable = false)
    private Usuario destinatario; // Adicionado para possibilitar a inserção de dados do destinatário

    /**
     * Valor original da remessa em BRL.
     *
     * Valor informado pelo usuário na moeda de origem (Real brasileiro).
     * Usado como base para cálculo de taxas e conversão para moeda de destino.
     *
     * Precisão: 10 dígitos com 2 casas decimais (formato monetário brasileiro).
     */
    @Column(nullable = false)
    private BigDecimal valor;

    /**
     * Taxa cobrada pela operação em BRL.
     *
     * Valor da taxa calculada conforme tipo de usuário:
     * - Pessoa Física (PF): 2% sobre o valor
     * - Pessoa Jurídica (PJ): 1% sobre o valor
     *
     * Taxa é descontada do saldo BRL do remetente.
     * Precisão: 10 dígitos com 4 casas decimais para cálculos precisos.
     */
    @Column(nullable = false)
    private BigDecimal taxa;

    /**
     * Moeda de destino da conversão.
     *
     * Código ISO 4217 da moeda para qual o valor será convertido.
     * Atualmente suporta apenas "USD" (Dólar americano).
     *
     * Validação: formato de 3 caracteres maiúsculos (padrão ISO).
     */
    @Column(name = "moeda_destino", nullable = false)
    private String moedaDestino;

    /**
     * Cotação aplicada na conversão BRL → Moeda Destino.
     *
     * Cotação oficial obtida da API do Banco Central do Brasil
     * no momento da transação. Em finais de semana, utiliza
     * a última cotação útil disponível.
     *
     * Representa quantas unidades de BRL equivalem a 1 unidade
     * da moeda de destino (ex: 1 USD = 5.0000 BRL).
     *
     * Precisão: 10 dígitos com 4 casas decimais para máxima precisão.
     */
    @Column(nullable = false)
    private BigDecimal cotacao;

    /**
     * Timestamp de criação da remessa.
     *
     * Data e hora exata de quando a transação foi processada.
     * Usado para:
     * - Controle de limites diários
     * - Consultas históricas por período
     * - Auditoria e compliance
     * - Ordenação cronológica de transações
     */
    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    /**
     * Valor final convertido na moeda de destino.
     *
     * Resultado da conversão: (valor ÷ cotacao)
     * Representa o montante que será creditado na carteira
     * do destinatário na moeda de destino.
     *
     * Cálculo: valor BRL ÷ cotação = valor USD
     * Exemplo: R$ 1000,00 ÷ 5,0000 = US$ 200,00
     *
     * Precisão: 10 dígitos com 4 casas decimais para moedas internacionais.
     */
    @Column(nullable = false)
    private BigDecimal valorConvertido; // Adicionado ao banco e configurado no builder
}