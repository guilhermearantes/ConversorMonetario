package com.guilherme.desafiointer;

import com.guilherme.desafiointer.config.TestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
class DesafioJavaInterApplicationTests {

    @Test
    @DisplayName("Deve carregar o contexto da aplicação")
    void contextLoads() {
        // O teste passa se o contexto for carregado com sucesso
    }
}
