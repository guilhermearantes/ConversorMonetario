package com.guilherme.desafiointer.integration;

import com.guilherme.desafiointer.config.TestConfig;
import com.guilherme.desafiointer.domain.*;
import com.guilherme.desafiointer.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
class CarteiraUsuarioIntegrationTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CarteiraRepository carteiraRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // ... resto do código ...

    @AfterEach
    void limparDados() {
        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE remessas").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE carteiras").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE usuarios").executeUpdate();
        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
        entityManager.flush();
    }
}