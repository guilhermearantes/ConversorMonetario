package com.guilherme.desafiointer.service.strategy;

import com.guilherme.desafiointer.config.constants.AppConstants;
import com.guilherme.desafiointer.domain.Usuario;
import com.guilherme.desafiointer.exception.domain.LimiteDiarioExcedidoException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class LimiteDiarioPJValidator implements LimiteDiarioValidator {

    @Override
    public void validar(Usuario usuario, BigDecimal valorTotalDiario, BigDecimal valorNovaTransacao) {
        if (valorTotalDiario.add(valorNovaTransacao).compareTo(AppConstants.LIMITE_DIARIO_PJ) > 0) {
            throw new LimiteDiarioExcedidoException("Limite diário de R$50.000,00 excedido para Pessoa Jurídica.");
        }
    }
}