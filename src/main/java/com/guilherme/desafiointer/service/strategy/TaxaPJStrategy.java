package com.guilherme.desafiointer.service.strategy;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class TaxaPJStrategy implements TaxaStrategy {
    private static final BigDecimal TAXA_PJ = new BigDecimal("0.01"); // 1%

    @Override
    public BigDecimal calcularTaxa(BigDecimal valor) {
        return valor.multiply(TAXA_PJ).setScale(2, RoundingMode.HALF_UP);
    }
}