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
                    new BigDecimal("1000.00"),
                    new BigDecimal("500.00")
            );
            entityManager.flush();
            entityManager.clear();

            // Act
            Optional<Carteira> carteira = carteiraRepository.findByUsuarioId(usuario.getId());

            // Assert
            assertTrue(carteira.isPresent());
            assertAll("Verificações da carteira",
                    () -> assertNotNull(carteira.get()),
                    () -> assertEquals(new BigDecimal("1000.00"), carteira.get().getSaldoBRL()),
                    () -> assertEquals(new BigDecimal("500.00"), carteira.get().getSaldoUSD()),
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
                    new BigDecimal("1000.00"),
                    new BigDecimal("500.00")
            );
            entityManager.flush();
            entityManager.clear();

            // Act
            Optional<Carteira> carteira = carteiraRepository.findByUsuarioIdWithPessimisticLock(usuario.getId());

            // Assert
            assertTrue(carteira.isPresent());
            assertAll("Verificações da carteira com lock",
                    () -> assertNotNull(carteira.get()),
                    () -> assertEquals(new BigDecimal("1000.00"), carteira.get().getSaldoBRL()),
                    () -> assertEquals(new BigDecimal("500.00"), carteira.get().getSaldoUSD()),
                    () -> assertEquals(usuario.getId(), carteira.get().getUsuario().getId())
            );
        }
    }

    @Nested
    @DisplayName("Testes de operações financeiras")
    class OperacoesFinanceirasTests {

        @Test
        @DisplayName("Deve debitar saldo BRL da carteira")
        void deveDebitarSaldoBRLDaCarteira() {
            // Arrange
            Usuario usuario = criarUsuarioPF(
                    "João Silva",
                    "joao@email.com",
                    CPF_VALIDO,
                    new BigDecimal("1000.00"),
                    new BigDecimal("500.00")
            );

            Carteira carteira = usuario.getCarteira();
            carteira.debitar(new BigDecimal("500.00"), "BRL");

            // Act
            carteiraRepository.save(carteira);
            entityManager.flush();
            entityManager.clear();

            // Assert
            Carteira carteiraAtualizada = carteiraRepository.findByUsuarioId(usuario.getId()).get();
            assertEquals(new BigDecimal("500.00"), carteiraAtualizada.getSaldoBRL());
        }

        @Test
        @DisplayName("Deve debitar saldo USD da carteira")
        void deveDebitarSaldoUSDDaCarteira() {
            // Arrange
            Usuario usuario = criarUsuarioPF(
                    "João Silva",
                    "joao@email.com",
                    CPF_VALIDO,
                    new BigDecimal("1000.00"),
                    new BigDecimal("500.00")
            );

            Carteira carteira = usuario.getCarteira();
            carteira.debitar(new BigDecimal("200.00"), "USD");

            // Act
            carteiraRepository.save(carteira);
            entityManager.flush();
            entityManager.clear();

            // Assert
            Carteira carteiraAtualizada = carteiraRepository.findByUsuarioId(usuario.getId()).get();
            assertEquals(new BigDecimal("300.00"), carteiraAtualizada.getSaldoUSD());
        }

        @Test
        @DisplayName("Deve creditar saldo BRL na carteira")
        void deveCreditarSaldoBRLNaCarteira() {
            // Arrange
            Usuario usuario = criarUsuarioPF(
                    "João Silva",
                    "joao@email.com",
                    CPF_VALIDO,
                    new BigDecimal("1000.00"),
                    new BigDecimal("500.00")
            );

            Carteira carteira = usuario.getCarteira();
            carteira.creditar(new BigDecimal("200.00"), "BRL");

            // Act
            carteiraRepository.save(carteira);
            entityManager.flush();
            entityManager.clear();

            // Assert
            Carteira carteiraAtualizada = carteiraRepository.findByUsuarioId(usuario.getId()).get();
            assertEquals(new BigDecimal("1200.00"), carteiraAtualizada.getSaldoBRL());
        }

        @Test
        @DisplayName("Deve creditar saldo USD na carteira")
        void deveCreditarSaldoUSDNaCarteira() {
            // Arrange
            Usuario usuario = criarUsuarioPF(
                    "João Silva",
                    "joao@email.com",
                    CPF_VALIDO,
                    new BigDecimal("1000.00"),
                    new BigDecimal("500.00")
            );

            Carteira carteira = usuario.getCarteira();
            carteira.creditar(new BigDecimal("100.00"), "USD");

            // Act
            carteiraRepository.save(carteira);
            entityManager.flush();
            entityManager.clear();

            // Assert
            Carteira carteiraAtualizada = carteiraRepository.findByUsuarioId(usuario.getId()).get();
            assertEquals(new BigDecimal("600.00"), carteiraAtualizada.getSaldoUSD());
        }
    }

    private Usuario criarUsuarioPF(String nome, String email, String documento,
                                   BigDecimal saldoInicialBRL, BigDecimal saldoInicialUSD) {
        Usuario usuario = Usuario.builder()
                .nomeCompleto(nome)
                .email(email)
                .documento(documento)
                .tipoUsuario(TipoUsuario.PF)
                .senha("Senha@123")
                .build();

        entityManager.persist(usuario);

        Carteira carteira = Carteira.builder()
                .saldoBRL(saldoInicialBRL)
                .saldoUSD(saldoInicialUSD)
                .usuario(usuario)
                .build();

        entityManager.persist(carteira);
        usuario.setCarteira(carteira);
        return entityManager.merge(usuario);
    }
}