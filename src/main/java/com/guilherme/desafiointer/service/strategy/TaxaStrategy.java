package com.guilherme.desafiointer.service.strategy;

import java.math.BigDecimal;

/**
 * Interface Strategy para cálculo diferenciado de taxas por tipo de usuário.
 *
 * Define o contrato comum para implementações específicas de cálculo de taxa
 * em remessas internacionais, permitindo diferentes algoritmos baseados no
 * tipo de usuário (PF/PJ) através do Strategy Pattern.
 *
 * Implementações disponíveis:
 * - TaxaPFStrategy: 2% para Pessoa Física
 * - TaxaPJStrategy: 1% para Pessoa Jurídica
 *
 * Características do contrato:
 * - Entrada: BigDecimal (valor base da remessa)
 * - Saída: BigDecimal (taxa calculada com 2 casas decimais)
 * - Arredondamento: HALF_UP (comercial)
 * - Thread-safe: Implementações devem ser stateless
 *
 * Integração:
 * - StrategyFactory: Seleção automática por TipoUsuario
 * - RemessaProcessor: Aplicação durante processamento
 * - Spring Context: Gerenciamento via @Component
 *
 * Padrões aplicados:
 * - Strategy Pattern: Algoritmos intercambiáveis
 * - Dependency Injection: Inversão de controle
 * - Open/Closed Principle: Extensível sem modificar código existente
 *
 * @see TaxaPFStrategy - Implementação para Pessoa Física (2%)
 * @see TaxaPJStrategy - Implementação para Pessoa Jurídica (1%)
 * @see StrategyFactory - Factory para seleção de estratégias
 */
public interface TaxaStrategy {
    BigDecimal calcularTaxa(BigDecimal valor);
}