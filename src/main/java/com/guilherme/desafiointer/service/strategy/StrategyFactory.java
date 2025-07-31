package com.guilherme.desafiointer.service.strategy;

import com.guilherme.desafiointer.domain.TipoUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Fábrica de estratégias para processamento diferenciado por tipo de usuário.
 *
 * Implementa Factory Pattern para fornecer validadores e calculadoras
 * específicas baseadas no tipo de usuário (PF/PJ), centralizando
 * a lógica de seleção de estratégias.
 */
@Component
@RequiredArgsConstructor
public class StrategyFactory {

    private final LimiteDiarioPFValidator limiteDiarioPFValidator;
    private final LimiteDiarioPJValidator limiteDiarioPJValidator;
    private final TaxaPFStrategy taxaPFStrategy;
    private final TaxaPJStrategy taxaPJStrategy;

    /**
     * Retorna validador de limite diário baseado no tipo de usuário.
     * PF: R$ 10.000 | PJ: R$ 50.000
     */
    public LimiteDiarioValidator getLimiteValidator(TipoUsuario tipoUsuario) {
        return switch (tipoUsuario) {
            case PF -> limiteDiarioPFValidator;
            case PJ -> limiteDiarioPJValidator;
        };
    }

    /**
     * Retorna calculadora de taxa baseada no tipo de usuário.
     * PF: 3% | PJ: 1,5%
     */
    public TaxaStrategy getTaxaStrategy(TipoUsuario tipoUsuario) {
        return switch (tipoUsuario) {
            case PF -> taxaPFStrategy;
            case PJ -> taxaPJStrategy;
        };
    }
}