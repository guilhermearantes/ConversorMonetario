package com.guilherme.desafiointer.service.strategy;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Estratégia de cálculo de taxa para Pessoa Física (PF).
 * Implementa Strategy Pattern com taxa fixa de 2% sobre valor da remessa.
 */
@Component
public class TaxaPFStrategy implements TaxaStrategy {

    /** Taxa aplicada: 2% sobre valor transferido */
    private static final BigDecimal TAXA_PF = new BigDecimal("0.02"); // 2%

    /**
     * Calcula taxa PF: valor × 2% com arredondamento comercial.
     * @param valor montante base para cálculo
     * @return taxa em BigDecimal com 2 casas decimais
     */
    @Override
    public BigDecimal calcularTaxa(BigDecimal valor) {
        return valor.multiply(TAXA_PF).setScale(2, RoundingMode.HALF_UP);
    }
}