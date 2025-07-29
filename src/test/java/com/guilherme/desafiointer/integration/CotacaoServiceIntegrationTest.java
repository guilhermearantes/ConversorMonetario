package com.guilherme.desafiointer.integration;

import com.guilherme.desafiointer.config.TestConfig;
import com.guilherme.desafiointer.service.impl.CotacaoServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("integration-test")
@Import(TestConfig.class)
@DisplayName("Testes de Integração - API de Cotação BCB")
class CotacaoServiceIntegrationTest {

    @Autowired
    private CotacaoServiceImpl cotacaoService;

    @Test
    @DisplayName("Deve obter cotação do dólar com sucesso em dia útil")
    void deveObterCotacaoDolarComSucesso() {
        // Verifica se hoje é dia útil
        if (isDiaUtil(LocalDate.now())) {
            // Act
            BigDecimal cotacao = cotacaoService.obterCotacao("USD");

            // Assert
            assertAll("Validação da cotação do dólar",
                    () -> assertNotNull(cotacao, "A cotação não deve ser nula"),
                    () -> assertTrue(cotacao.compareTo(BigDecimal.ZERO) > 0,
                            "A cotação deve ser maior que zero"),
                    () -> assertTrue(cotacao.scale() <= 4,
                            "A cotação deve ter no máximo 4 casas decimais")
            );
        } else {
            System.out.println("Teste ignorado por ser fim de semana");
        }
    }

    @Test
    @DisplayName("Deve obter última cotação disponível no fim de semana")
    void deveObterUltimaCotacaoNoFimDeSemana() {
        // Somente executa o teste se hoje for fim de semana
        if (!isDiaUtil(LocalDate.now())) {
            // Act
            BigDecimal cotacao = cotacaoService.obterCotacao("USD");
            BigDecimal segundaCotacao = cotacaoService.obterCotacao("USD");

            // Assert
            assertAll("Validação da cotação no fim de semana",
                    () -> assertNotNull(cotacao, "A cotação não deve ser nula"),
                    () -> assertTrue(cotacao.compareTo(BigDecimal.ZERO) > 0,
                            "A cotação deve ser maior que zero"),
                    () -> assertEquals(cotacao, segundaCotacao,
                            "Cotações subsequentes devem retornar o mesmo valor")
            );
        } else {
            System.out.println("Teste ignorado por não ser fim de semana");
        }
    }

    @Test
    @DisplayName("Deve falhar ao tentar obter cotação de moeda não suportada")
    void deveFalharAoTentarObterCotacaoMoedaNaoSuportada() {
        assertThrows(IllegalArgumentException.class,
                () -> cotacaoService.obterCotacao("EUR"),
                "Deve lançar exceção para moeda não suportada");
    }

    private boolean isDiaUtil(LocalDate data) {
        return data.getDayOfWeek() != DayOfWeek.SATURDAY
                && data.getDayOfWeek() != DayOfWeek.SUNDAY;
    }
}