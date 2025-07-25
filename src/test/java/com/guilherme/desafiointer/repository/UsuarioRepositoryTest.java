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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DisplayName("Testes do UsuarioRepository")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
class UsuarioRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Nested
    @DisplayName("Testes de persistência de usuário")
    class PersistenciaTests {

        @Test
        @DisplayName("Deve salvar usuário PF com carteira")
        void deveSalvarUsuarioPFComCarteira() {
            Usuario usuario = criarUsuarioComCarteira("João Silva", TipoUsuario.PF,
                    "529.982.247-25", "joao.silva@email.com",
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

            Usuario usuarioSalvo = usuarioRepository.save(usuario);
            entityManager.flush();
            entityManager.clear();

            Usuario usuarioRecuperado = entityManager.find(Usuario.class, usuarioSalvo.getId());
            assertAll("Verificações do usuário PF salvo",
                    () -> assertNotNull(usuarioRecuperado.getId()),
                    () -> assertEquals("529.982.247-25", usuarioRecuperado.getDocumento()),
                    () -> assertEquals("João Silva", usuarioRecuperado.getNomeCompleto()),
                    () -> assertEquals(TipoUsuario.PF, usuarioRecuperado.getTipoUsuario()),
                    () -> assertNotNull(usuarioRecuperado.getCarteira()),
                    () -> assertEquals(new BigDecimal("0.00"), usuarioRecuperado.getCarteira().getSaldo())
            );
        }

        @Test
        @DisplayName("Deve salvar usuário PJ com carteira")
        void deveSalvarUsuarioPJComCarteira() {
            Usuario usuario = criarUsuarioComCarteira("Empresa LTDA", TipoUsuario.PJ,
                    "45.997.418/0001-53", "empresa@email.com",
                    new BigDecimal("1000.00"));

            Usuario usuarioSalvo = usuarioRepository.save(usuario);
            entityManager.flush();
            entityManager.clear();

            Usuario usuarioRecuperado = entityManager.find(Usuario.class, usuarioSalvo.getId());
            assertAll("Verificações do usuário PJ salvo",
                    () -> assertNotNull(usuarioRecuperado.getId()),
                    () -> assertEquals("45.997.418/0001-53", usuarioRecuperado.getDocumento()),
                    () -> assertEquals("Empresa LTDA", usuarioRecuperado.getNomeCompleto()),
                    () -> assertEquals(TipoUsuario.PJ, usuarioRecuperado.getTipoUsuario()),
                    () -> assertNotNull(usuarioRecuperado.getCarteira()),
                    () -> assertEquals(new BigDecimal("1000.00"), usuarioRecuperado.getCarteira().getSaldo())
            );
        }
    }

    @Nested
    @DisplayName("Testes de busca de usuário")
    class BuscaTests {

        @Test
        @DisplayName("Deve buscar usuário por documento")
        void deveBuscarUsuarioPorDocumento() {
            Usuario usuario = criarUsuarioComCarteira("João Silva", TipoUsuario.PF,
                    "529.982.247-25", "joao.silva@email.com", BigDecimal.ZERO);

            entityManager.persist(usuario);
            entityManager.flush();
            entityManager.clear();

            var usuarioEncontrado = usuarioRepository.findByDocumento("529.982.247-25");
            assertTrue(usuarioEncontrado.isPresent());
            assertAll("Verificações do usuário encontrado",
                    () -> assertEquals("João Silva", usuarioEncontrado.get().getNomeCompleto()),
                    () -> assertNotNull(usuarioEncontrado.get().getCarteira())
            );
        }

        @Test
        @DisplayName("Deve retornar vazio ao buscar documento inexistente")
        void deveRetornarVazioAoBuscarDocumentoInexistente() {
            var usuarioEncontrado = usuarioRepository.findByDocumento("999.999.999-99");
            assertTrue(usuarioEncontrado.isEmpty());
        }
    }

    @Nested
    @DisplayName("Testes de validação de existência")
    class ValidacaoExistenciaTests {

        @Test
        @DisplayName("Deve verificar existência de documento")
        void deveVerificarExistenciaDocumento() {
            Usuario usuario = criarUsuarioComCarteira("João Silva", TipoUsuario.PF,
                    "529.982.247-25", "joao.silva@email.com", BigDecimal.ZERO);

            entityManager.persist(usuario);
            entityManager.flush();

            assertAll("Verificações de existência de documento",
                    () -> assertTrue(usuarioRepository.existsByDocumento("529.982.247-25")),
                    () -> assertFalse(usuarioRepository.existsByDocumento("099.999.999-99"))
            );
        }

        @Test
        @DisplayName("Deve verificar existência de email")
        void deveVerificarExistenciaEmail() {
            Usuario usuario = criarUsuarioComCarteira("João Silva", TipoUsuario.PF,
                    "529.982.247-25", "joao.silva@email.com", BigDecimal.ZERO);

            entityManager.persist(usuario);
            entityManager.flush();

            assertAll("Verificações de existência de email",
                    () -> assertTrue(usuarioRepository.existsByEmail("joao.silva@email.com")),
                    () -> assertFalse(usuarioRepository.existsByEmail("outro@email.com"))
            );
        }
    }

    @Nested
    @DisplayName("Testes de restrições de unicidade")
    class RestricaoUnicidadeTests {

        @Test
        @DisplayName("Não deve permitir dois usuários com mesmo documento")
        void naoDevePermitirUsuariosComMesmoDocumento() {
            Usuario usuario1 = criarUsuarioComCarteira("João Silva", TipoUsuario.PF,
                    "529.982.247-25", "joao.silva@email.com", BigDecimal.ZERO);
            usuarioRepository.save(usuario1);

            Usuario usuario2 = criarUsuarioComCarteira("João Silva Segundo", TipoUsuario.PF,
                    "529.982.247-25", "joao.silva2@email.com", BigDecimal.ZERO);

            assertThrows(DataIntegrityViolationException.class, () -> {
                usuarioRepository.save(usuario2);
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("Não deve permitir dois usuários com mesmo email")
        void naoDevePermitirUsuariosComMesmoEmail() {
            Usuario usuario1 = criarUsuarioComCarteira("João Silva", TipoUsuario.PF,
                    "529.982.247-25", "joao.silva@email.com", BigDecimal.ZERO);
            usuarioRepository.save(usuario1);
            entityManager.flush();

            Usuario usuario2 = criarUsuarioComCarteira("João Silva Segundo", TipoUsuario.PF,
                    "529.982.247-25", "joao.silva@email.com", BigDecimal.ZERO);

            assertThrows(DataIntegrityViolationException.class, () -> {
                usuarioRepository.save(usuario2);
                entityManager.flush();
            });
        }
    }

    private Usuario criarUsuarioComCarteira(String nome, TipoUsuario tipo,
                                            String documento, String email, BigDecimal saldoInicial) {
        Usuario usuario = Usuario.builder()
                .nomeCompleto(nome)
                .tipoUsuario(tipo)
                .documento(documento)
                .email(email)
                .senha("Senha@123")
                .build();

        Carteira carteira = Carteira.builder()
                .saldo(saldoInicial)
                .usuario(usuario)
                .build();

        usuario.setCarteira(carteira);
        return usuario;
    }
}