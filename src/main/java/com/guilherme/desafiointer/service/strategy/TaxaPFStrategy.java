package com.guilherme.desafiointer.service.strategy;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class TaxaPFStrategy implements TaxaStrategy {
    private static final BigDecimal TAXA_PF = new BigDecimal("0.02"); // 2%

    @Override
    public BigDecimal calcularTaxa(BigDecimal valor) {
        return valor.multiply(TAXA_PF).setScale(2, RoundingMode.HALF_UP);
    }
}