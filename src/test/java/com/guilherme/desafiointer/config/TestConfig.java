package com.guilherme.desafiointer.config;

import com.guilherme.desafiointer.service.CotacaoService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;

@Configuration
public class TestConfig {

    @Bean
    @Primary
    public CotacaoService cotacaoService() {
        CotacaoService mockCotacaoService = Mockito.mock(CotacaoService.class);
        // Configura um valor padrão para cotação em USD
        Mockito.when(mockCotacaoService.obterCotacao("USD"))
                .thenReturn(new BigDecimal("5.00"));
        // Você pode adicionar mais configurações para outras moedas se necessário
        return mockCotacaoService;
    }
}