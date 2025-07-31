package com.guilherme.desafiointer.service.strategy;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Estratégia de cálculo de taxa para Pessoa Jurídica (PJ).
 * Implementa Strategy Pattern com taxa fixa de 1% sobre valor da remessa.
 */
@Component
public class TaxaPJStrategy implements TaxaStrategy {

    /** Taxa aplicada: 1% sobre valor transferido */
    private static final BigDecimal TAXA_PJ = new BigDecimal("0.01"); // 1%

    /**
     * Calcula taxa PJ: valor × 1% com arredondamento comercial.
     * @param valor montante base para cálculo
     * @return taxa em BigDecimal com 2 casas decimais
     */
    @Override
    public BigDecimal calcularTaxa(BigDecimal valor) {
        return valor.multiply(TAXA_PJ).setScale(2, RoundingMode.HALF_UP);
    }
}