package com.guilherme.desafiointer.config.constants;

import lombok.experimental.UtilityClass;
import java.math.BigDecimal;
import java.util.Set;

@UtilityClass
public class AppConstants {

    // Constantes monetárias
    public static final int DECIMAL_SCALE = 4;
    public static final BigDecimal TAXA_MINIMA = BigDecimal.valueOf(0.01);
    public static final BigDecimal LIMITE_DIARIO_PF = BigDecimal.valueOf(10000.00);
    public static final BigDecimal LIMITE_DIARIO_PJ = BigDecimal.valueOf(50000.00);

    // Moedas
    public static final Set<String> MOEDAS_SUPORTADAS = Set.of("USD");
    public static final String MOEDA_PADRAO = "USD";

    // Formatação de datas
    public static final String DATE_FORMAT = "MM-dd-yyyy";

    // Paginação
    public static final int TAMANHO_PAGINA_PADRAO = 20;
    public static final int TAMANHO_MAXIMO_PAGINA = 100;

    // Períodos
    public static final int PERIODO_MAXIMO_HISTORICO_DIAS = 90;

    // Cache
    public static final String CACHE_COTACOES = "cotacoes";
    public static final String CACHE_HISTORICO = "historicoTransacoes";
    public static final String CACHE_TOTAIS = "totaisTransacoes";
}