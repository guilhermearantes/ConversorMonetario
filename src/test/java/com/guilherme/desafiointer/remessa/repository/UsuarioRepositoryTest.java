package com.guilherme.desafiointer.remessa.repository;

import com.guilherme.desafiointer.remessa.domain.Usuario;
import com.guilherme.desafiointer.remessa.domain.TipoUsuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UsuarioRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void deveSalvarUsuarioPF() {
        Usuario usuario = Usuario.builder()
                .nomeCompleto("João Silva")
                .tipoUsuario(TipoUsuario.PF)
                .documento("529.982.247-25")  // CPF válido
                .email("joao.silva@email.com")
                .senha("Senha@123")  // Senha mais forte
                .build();

        Usuario usuarioSalvo = usuarioRepository.save(usuario);

        assertNotNull(usuarioSalvo.getId());
        assertEquals("529.982.247-25", usuarioSalvo.getDocumento());
        assertEquals("João Silva", usuarioSalvo.getNomeCompleto());
        assertEquals(TipoUsuario.PF, usuarioSalvo.getTipoUsuario());
    }

    @Test
    void deveSalvarUsuarioPJ() {
        Usuario usuario = Usuario.builder()
                .nomeCompleto("Empresa LTDA")
                .tipoUsuario(TipoUsuario.PJ)
                .documento("45.997.418/0001-53")  // CNPJ válido
                .email("empresa@email.com")
                .senha("Senha@123")
                .build();

        Usuario usuarioSalvo = usuarioRepository.save(usuario);

        assertNotNull(usuarioSalvo.getId());
        assertEquals("45.997.418/0001-53", usuarioSalvo.getDocumento());
        assertEquals("Empresa LTDA", usuarioSalvo.getNomeCompleto());
        assertEquals(TipoUsuario.PJ, usuarioSalvo.getTipoUsuario());
    }

    @Test
    void deveBuscarUsuarioPorDocumento() {
        Usuario usuario = Usuario.builder()
                .nomeCompleto("João Silva")
                .tipoUsuario(TipoUsuario.PF)
                .documento("529.982.247-25")
                .email("joao.silva@email.com")
                .senha("Senha@123")
                .build();

        entityManager.persist(usuario);
        entityManager.flush();

        var usuarioEncontrado = usuarioRepository.findByDocumento("529.982.247-25");
        assertTrue(usuarioEncontrado.isPresent());
        assertEquals("João Silva", usuarioEncontrado.get().getNomeCompleto());
    }

    @Test
    void deveVerificarSeDocumentoExiste() {
        Usuario usuario = Usuario.builder()
                .nomeCompleto("João Silva")
                .tipoUsuario(TipoUsuario.PF)
                .documento("529.982.247-25")
                .email("joao.silva@email.com")
                .senha("Senha@123")
                .build();

        entityManager.persist(usuario);
        entityManager.flush();

        assertTrue(usuarioRepository.existsByDocumento("529.982.247-25"));
        assertFalse(usuarioRepository.existsByDocumento("099.999.999-99"));
    }

    @Test
    void deveVerificarSeEmailExiste() {
        Usuario usuario = Usuario.builder()
                .nomeCompleto("João Silva")
                .tipoUsuario(TipoUsuario.PF)
                .documento("529.982.247-25")
                .email("joao.silva@email.com")
                .senha("Senha@123")
                .build();

        entityManager.persist(usuario);
        entityManager.flush();

        assertTrue(usuarioRepository.existsByEmail("joao.silva@email.com"));
        assertFalse(usuarioRepository.existsByEmail("outro@email.com"));
    }

    @Test
    void naoDeveSalvarDoisUsuariosComMesmoDocumento() {
        Usuario usuario1 = Usuario.builder()
                .nomeCompleto("João Silva")
                .tipoUsuario(TipoUsuario.PF)
                .documento("529.982.247-25")
                .email("joao.silva@email.com")
                .senha("Senha@123")
                .build();

        usuarioRepository.save(usuario1);

        Usuario usuario2 = Usuario.builder()
                .nomeCompleto("João Silva Segundo")
                .tipoUsuario(TipoUsuario.PF)
                .documento("529.982.247-25")  // Mesmo documento
                .email("joao.silva2@email.com")
                .senha("Senha@123")
                .build();

        assertThrows(Exception.class, () -> {
            usuarioRepository.save(usuario2);
            usuarioRepository.flush();
        });
    }

    @Test
    void naoDeveSalvarDoisUsuariosComMesmoEmail() {
        Usuario usuario1 = Usuario.builder()
                .nomeCompleto("João Silva")
                .tipoUsuario(TipoUsuario.PF)
                .documento("529.982.247-25")
                .email("joao.silva@email.com")
                .senha("Senha@123")
                .build();

        usuarioRepository.save(usuario1);

        Usuario usuario2 = Usuario.builder()
                .nomeCompleto("João Silva Segundo")
                .tipoUsuario(TipoUsuario.PF)
                .documento("857.486.350-70")  // CPF diferente
                .email("joao.silva@email.com")  // Mesmo email
                .senha("Senha@123")
                .build();

        assertThrows(Exception.class, () -> {
            usuarioRepository.save(usuario2);
            usuarioRepository.flush();
        });
    }
}
