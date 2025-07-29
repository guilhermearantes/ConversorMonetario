
package com.guilherme.desafiointer.service.strategy;

import com.guilherme.desafiointer.domain.TipoUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class StrategyFactory {

    private final LimiteDiarioPFValidator limiteDiarioPFValidator;
    private final LimiteDiarioPJValidator limiteDiarioPJValidator;
    private final TaxaPFStrategy taxaPFStrategy;
    private final TaxaPJStrategy taxaPJStrategy;

    public LimiteDiarioValidator getLimiteValidator(TipoUsuario tipoUsuario) {
        return switch (tipoUsuario) {
            case PF -> limiteDiarioPFValidator;
            case PJ -> limiteDiarioPJValidator;
        };
    }

    public TaxaStrategy getTaxaStrategy(TipoUsuario tipoUsuario) {
        return switch (tipoUsuario) {
            case PF -> taxaPFStrategy;
            case PJ -> taxaPJStrategy;
        };
    }
}