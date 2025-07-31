package com.guilherme.desafiointer.service.strategy;

import com.guilherme.desafiointer.domain.Usuario;
import java.math.BigDecimal;

public interface LimiteDiarioValidator {
    void validar(Usuario usuario, BigDecimal valorTotalDiario, BigDecimal valorNovaTransacao);
}