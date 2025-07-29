package com.guilherme.desafiointer.service.strategy;

import java.math.BigDecimal;

public interface TaxaStrategy {
    BigDecimal calcularTaxa(BigDecimal valor);
}