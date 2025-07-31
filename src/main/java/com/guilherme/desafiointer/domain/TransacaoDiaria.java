package com.guilherme.desafiointer.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entidade para controle de limites diários de remessas por usuario.
 *
 * Consolida o valor total transacionado por usuario em cada data,
 * permitindo validação de limites regulamentares antes de novas operações.
 *
 * Limites: PF (R$ 10.000/dia) | PJ (R$ 50.000/dia)
 */
@Entity
@Table(name = "transacoes_diarias")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransacaoDiaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Usuário proprietário da consolidação diária
     */
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /**
     * Data de referência da consolidação
     */
    @Column(nullable = false)
    private LocalDate data;

    /**
     * Valor total consolidado das remessas do dia (BRL)
     */
    @Column(nullable = false)
    private BigDecimal valorTotal;

    /**
     * Atualiza o valor total das transações do dia de forma thread-safe.
     * Usado para incrementar valores quando novas remessas são processadas.
     *
     * @param novoTotal novo valor total (deve ser >= atual)
     */
    public void atualizarValorTotal(BigDecimal novoTotal) {
        this.valorTotal = novoTotal;
    }
}