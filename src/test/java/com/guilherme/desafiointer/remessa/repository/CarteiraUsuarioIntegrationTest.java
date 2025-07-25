package com.guilherme.desafiointer.remessa.repository;

import com.guilherme.desafiointer.remessa.DesafioJavaInterApplication;
import com.guilherme.desafiointer.remessa.repository.UsuarioRepository;
import com.guilherme.desafiointer.remessa.repository.CarteiraRepository;
import com.guilherme.desafiointer.remessa.domain.Usuario;
import com.guilherme.desafiointer.remessa.domain.Carteira;
import com.guilherme.desafiointer.remessa.domain.TipoUsuario;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;

@SpringBootTest(classes = DesafioJavaInterApplication.class)
@ActiveProfiles("test")
@Transactional
class CarteiraUsuarioIntegrationTest {



    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CarteiraRepository carteiraRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void deveSalvarUsuarioComCarteira() {
        Usuario usuario = Usuario.builder()
                .nomeCompleto("João Silva")
                .tipoUsuario(TipoUsuario.PF)
                .email("joao.silva@gmail.com")
                .senha("Senha@123")
                .documento("529.982.247-25")
                .build();

        usuario = usuarioRepository.save(usuario);

        Carteira carteira = Carteira.builder()
                .saldo(BigDecimal.ZERO)
                .usuario(usuario)
                .build();

        carteira = carteiraRepository.save(carteira);
        usuario.setCarteira(carteira);
        usuario = usuarioRepository.save(usuario);

        assertNotNull(carteira.getId());
        assertEquals(usuario.getId(), carteira.getUsuario().getId());
        assertEquals(BigDecimal.ZERO, carteira.getSaldo());
    }


    @Test
    void naoDeveSalvarDuasCarteirasParaMesmoUsuario() {
        // Cria e salva usuário
        Usuario usuario = Usuario.builder()
                .nomeCompleto("João Silva")
                .tipoUsuario(TipoUsuario.PF)
                .email("joao.silva2@gmail.com")
                .senha("Senha@123")
                .documento("529.982.247-25")
                .build();

        usuario = usuarioRepository.save(usuario);

        // Cria e salva primeira carteira
        Carteira carteira1 = Carteira.builder()
                .saldo(BigDecimal.ZERO)
                .usuario(usuario)
                .build();

        carteira1 = carteiraRepository.save(carteira1);
        usuario.setCarteira(carteira1);
        usuarioRepository.save(usuario);

        // Tenta criar segunda carteira
        Carteira carteira2 = Carteira.builder()
                .saldo(BigDecimal.ZERO)
                .usuario(usuario)
                .build();

        // Verifica se a exceção é lançada
        DataIntegrityViolationException exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> carteiraRepository.save(carteira2)
        );

        // Limpa a sessão após a exceção
        entityManager.clear();
    }

    @Test
    void deveSalvarUsuarioPJComCarteira() {
        Usuario usuario = Usuario.builder()
                .nomeCompleto("Empresa LTDA")
                .tipoUsuario(TipoUsuario.PJ)
                .email("empresa@gmail.com")
                .senha("Senha@123")
                .documento("45.997.418/0001-53")
                .build();

        usuario = usuarioRepository.save(usuario);

        Carteira carteira = Carteira.builder()
                .saldo(BigDecimal.ZERO)
                .usuario(usuario)
                .build();

        carteira = carteiraRepository.save(carteira);
        usuario.setCarteira(carteira);
        usuario = usuarioRepository.save(usuario);

        assertNotNull(carteira.getId());
        assertEquals(usuario.getId(), carteira.getUsuario().getId());
    }

    @AfterEach
    void limparDados() {
        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE carteiras").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE usuarios").executeUpdate();
        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
        entityManager.flush();
    }
}