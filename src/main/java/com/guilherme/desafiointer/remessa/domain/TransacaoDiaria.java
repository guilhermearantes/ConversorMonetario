
package com.guilherme.desafiointer.remessa.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transacoes_diarias")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransacaoDiaria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private BigDecimal valorTotal;

    public boolean excedeLimit() {
        return valorTotal.compareTo(usuario.getTipoUsuario().getLimiteDiario()) > 0;
    }

}