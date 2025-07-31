package com.guilherme.desafiointer.domain;

import com.guilherme.desafiointer.exception.domain.SaldoInsuficienteException;
import com.guilherme.desafiointer.repository.CarteiraRepository;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;

/**
 * Entidade que representa a carteira digital de um usuário.
 *
 * Gerencia saldos em múltiplas moedas (BRL e USD) e fornece
 * operações seguras de débito e crédito com validação de saldo.
 * Cada usuário possui uma carteira única associada.
 *
 * Características:
 * - Saldos separados para Real (BRL) e Dólar (USD)
 * - Operações atômicas de débito/crédito
 * - Validação de saldo insuficiente
 * - Relacionamento bidirecional com Usuario
 *
 * Regras de negócio:
 * - Não permite saldo negativo
 * - Suporte apenas para moedas BRL e USD
 * - Operações thread-safe via lock pessimista no repository
 *
 * Utilizada por:
 * - RemessaProcessor para transferências internacionais
 * - UsuarioService para inicialização de contas
 * - Validações de saldo em operações financeiras
 *
 * @see Usuario
 * @see CarteiraRepository
 * @see SaldoInsuficienteException
 */
@Entity
@Table(name = "carteiras")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class Carteira {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Saldo em Real brasileiro (BRL).
     * Valor sempre não-negativo após operações válidas.
     */
    @Column(nullable = false)
    private BigDecimal saldoBRL;

    /**
     * Saldo em Dólar americano (USD).
     * Valor sempre não-negativo após operações válidas.
     */
    @Column(nullable = false)
    private BigDecimal saldoUSD;

    /**
     * Usuário proprietário desta carteira.
     * Relacionamento obrigatório e único (OneToOne).
     */
    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /**
     * Debita um valor da carteira na moeda especificada.
     *
     * Realiza validação de saldo suficiente antes da operação,
     * lançando exceção se o saldo for insuficiente.
     *
     * @param valor valor a ser debitado (deve ser positivo)
     * @param moeda moeda da operação ("BRL" ou "USD")
     * @throws SaldoInsuficienteException quando saldo é insuficiente
     * @throws IllegalArgumentException quando moeda não é suportada
     */
    public void debitar(BigDecimal valor, String moeda) {
        if ("BRL".equalsIgnoreCase(moeda)) {
            if (this.saldoBRL.compareTo(valor) < 0) {
                throw new SaldoInsuficienteException(
                        String.format("Saldo insuficiente em %s para realizar a operação. Saldo atual: %s, Valor solicitado: %s",
                                moeda, this.saldoBRL, valor)
                );
            }
            this.saldoBRL = this.saldoBRL.subtract(valor);
        } else if ("USD".equalsIgnoreCase(moeda)) {
            if (this.saldoUSD.compareTo(valor) < 0) {
                throw new SaldoInsuficienteException(
                        String.format("Saldo insuficiente em %s para realizar a operação. Saldo atual: %s, Valor solicitado: %s",
                                moeda, this.saldoUSD, valor)
                );
            }
            this.saldoUSD = this.saldoUSD.subtract(valor);
        } else {
            throw new IllegalArgumentException("Moeda não suportada: " + moeda);
        }
    }

    /**
     * Credita um valor na carteira na moeda especificada.
     *
     * Adiciona o valor ao saldo da moeda correspondente.
     * Não há limite máximo para créditos.
     *
     * @param valor valor a ser creditado (deve ser positivo)
     * @param moeda moeda da operação ("BRL" ou "USD")
     * @throws IllegalArgumentException quando moeda não é suportada
     */
    public void creditar(BigDecimal valor, String moeda) {
        if ("BRL".equalsIgnoreCase(moeda)) {
            this.saldoBRL = this.saldoBRL.add(valor);
        } else if ("USD".equalsIgnoreCase(moeda)) {
            this.saldoUSD = this.saldoUSD.add(valor);
        } else {
            throw new IllegalArgumentException("Moeda não suportada: " + moeda);
        }
    }
}