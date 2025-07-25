package com.guilherme.desafiointer.repository;

import com.guilherme.desafiointer.domain.Carteira;
import com.guilherme.desafiointer.domain.TipoUsuario;
import com.guilherme.desafiointer.domain.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DisplayName("Testes do CarteiraRepository")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
class CarteiraRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CarteiraRepository carteiraRepository;

    private static final String CPF_VALIDO = "529.982.247-25";

    @Nested
    @DisplayName("Testes de busca de carteira")
    class BuscaTests {

        @Test
        @DisplayName("Deve encontrar carteira por usuário")
        void deveEncontrarCarteiraPorUsuario() {
            // Arrange
            Usuario usuario = criarUsuarioPF(
                    "João Silva",
                    "joao@email.com",
                    CPF_VALIDO,
                    new BigDecimal("1000.00")
            );
            entityManager.flush();
            entityManager.clear();

            // Act
            Optional<Carteira> carteira = carteiraRepository.findByUsuarioId(usuario.getId());

            // Assert
            assertTrue(carteira.isPresent());
            assertAll("Verificações da carteira",
                    () -> assertNotNull(carteira.get()),
                    () -> assertEquals(new BigDecimal("1000.00"), carteira.get().getSaldo()),
                    () -> assertEquals(usuario.getId(), carteira.get().getUsuario().getId())
            );
        }

        @Test
        @DisplayName("Deve retornar vazio ao buscar carteira de usuário inexistente")
        void deveRetornarVazioAoBuscarCarteiraDeUsuarioInexistente() {
            // Act
            Optional<Carteira> carteira = carteiraRepository.findByUsuarioId(999L);

            // Assert
            assertTrue(carteira.isEmpty());
        }

        @Test
        @DisplayName("Deve encontrar carteira por usuário com lock")
        void deveEncontrarCarteiraPorUsuarioComLock() {
            // Arrange
            Usuario usuario = criarUsuarioPF(
                    "João Silva",
                    "joao@email.com",
                    CPF_VALIDO,
                    new BigDecimal("1000.00")
            );
            entityManager.flush();
            entityManager.clear();

            // Act
            Optional<Carteira> carteira = carteiraRepository.findByUsuarioIdWithLock(usuario.getId());

            // Assert
            assertTrue(carteira.isPresent());
            assertAll("Verificações da carteira com lock",
                    () -> assertNotNull(carteira.get()),
                    () -> assertEquals(new BigDecimal("1000.00"), carteira.get().getSaldo()),
                    () -> assertEquals(usuario.getId(), carteira.get().getUsuario().getId())
            );
        }
    }

    @Nested
    @DisplayName("Testes de operações financeiras")
    class OperacoesFinanceirasTests {

        @Test
        @DisplayName("Deve atualizar saldo da carteira")
        void deveAtualizarSaldoDaCarteira() {
            // Arrange
            Usuario usuario = criarUsuarioPF(
                    "João Silva",
                    "joao@email.com",
                    CPF_VALIDO,
                    new BigDecimal("1000.00")
            );

            Carteira carteira = usuario.getCarteira();
            carteira.debitar(new BigDecimal("500.00"));

            // Act
            carteiraRepository.save(carteira);
            entityManager.flush();
            entityManager.clear();

            // Assert
            Carteira carteiraAtualizada = carteiraRepository.findByUsuarioId(usuario.getId()).get();
            assertEquals(new BigDecimal("500.00"), carteiraAtualizada.getSaldo());
        }
    }

    private Usuario criarUsuarioPF(String nome, String email, String documento, BigDecimal saldoInicial) {
        Usuario usuario = Usuario.builder()
                .nomeCompleto(nome)
                .email(email)
                .documento(documento)
                .tipoUsuario(TipoUsuario.PF)
                .senha("Senha@123")
                .build();

        entityManager.persist(usuario);

        Carteira carteira = Carteira.builder()
                .saldo(saldoInicial)
                .usuario(usuario)
                .build();

        entityManager.persist(carteira);
        usuario.setCarteira(carteira);
        return entityManager.merge(usuario);
    }
}