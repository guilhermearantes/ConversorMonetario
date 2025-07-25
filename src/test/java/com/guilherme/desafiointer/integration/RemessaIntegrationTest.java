package com.guilherme.desafiointer.integration;

import com.guilherme.desafiointer.domain.*;
import com.guilherme.desafiointer.dto.RemessaDTO;
import com.guilherme.desafiointer.exception.LimiteDiarioExcedidoException;
import com.guilherme.desafiointer.exception.SaldoInsuficienteException;
import com.guilherme.desafiointer.repository.*;
import com.guilherme.desafiointer.service.RemessaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RemessaIntegrationTest {

    @Autowired
    private RemessaService remessaService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CarteiraRepository carteiraRepository;

    @Autowired
    private RemessaRepository remessaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void deveRealizarRemessaComSucesso() {
        // Arrange
        Usuario usuario = criarUsuarioComSaldo(TipoUsuario.PF, new BigDecimal("1000.00"));
        RemessaDTO remessaDTO = RemessaDTO.builder()
                .usuarioId(usuario.getId())
                .valor(new BigDecimal("100.00"))
                .moedaDestino("USD")
                .build();

        // Act
        Remessa remessa = remessaService.realizarRemessa(remessaDTO);

        // Assert
        assertNotNull(remessa.getId());
        assertEquals(usuario.getId(), remessa.getUsuario().getId());
        assertTrue(remessa.getTaxa().compareTo(BigDecimal.ZERO) > 0);

        // Verifica saldo atualizado
        Carteira carteiraAtualizada = carteiraRepository.findById(usuario.getCarteira().getId()).get();
        BigDecimal saldoEsperado = new BigDecimal("1000.00")
                .subtract(remessa.getValor())
                .subtract(remessa.getTaxa());
        assertEquals(saldoEsperado, carteiraAtualizada.getSaldo());
    }

    @Test
    void deveRecusarRemessaPorSaldoInsuficiente() {
        // Arrange
        Usuario usuario = criarUsuarioComSaldo(TipoUsuario.PF, new BigDecimal("100.00"));
        RemessaDTO remessaDTO = RemessaDTO.builder()
                .usuarioId(usuario.getId())
                .valor(new BigDecimal("1000.00"))
                .moedaDestino("USD")
                .build();

        // Act & Assert
        assertThrows(SaldoInsuficienteException.class,
                () -> remessaService.realizarRemessa(remessaDTO));

        // Verifica que saldo não foi alterado
        Carteira carteiraAtualizada = carteiraRepository.findById(usuario.getCarteira().getId()).get();
        assertEquals(new BigDecimal("100.00"), carteiraAtualizada.getSaldo());
    }

    @Test
    void deveRecusarRemessaPorLimiteDiario() {
        // Arrange
        Usuario usuario = criarUsuarioComSaldo(TipoUsuario.PF, new BigDecimal("50000.00"));
        BigDecimal valorAcimaLimite = TipoUsuario.PF.getLimiteDiario().add(BigDecimal.ONE);

        RemessaDTO remessaDTO = RemessaDTO.builder()
                .usuarioId(usuario.getId())
                .valor(valorAcimaLimite)
                .moedaDestino("USD")
                .build();

        // Act & Assert
        assertThrows(LimiteDiarioExcedidoException.class,
                () -> remessaService.realizarRemessa(remessaDTO));
    }

    @Test
    void deveCobrarTaxaDiferenciadaPorTipoUsuario() {
        // Arrange
        Usuario usuarioPF = criarUsuarioComSaldo(TipoUsuario.PF, new BigDecimal("1000.00"));
        Usuario usuarioPJ = criarUsuarioComSaldo(TipoUsuario.PJ, new BigDecimal("1000.00"));

        BigDecimal valorRemessa = new BigDecimal("100.00");

        // Act
        Remessa remessaPF = remessaService.realizarRemessa(createRemessaDTO(usuarioPF.getId(), valorRemessa));
        Remessa remessaPJ = remessaService.realizarRemessa(createRemessaDTO(usuarioPJ.getId(), valorRemessa));

        // Assert
        assertTrue(remessaPF.getTaxa().compareTo(remessaPJ.getTaxa()) > 0);
    }

    private Usuario criarUsuarioComSaldo(TipoUsuario tipo, BigDecimal saldo) {
        String email = tipo == TipoUsuario.PF ? "pf@email.com" : "pj@email.com";

        Usuario usuario = Usuario.builder()
                .nomeCompleto("Teste")
                .tipoUsuario(tipo)
                .email(email)
                .senha("senha123")
                .documento(tipo == TipoUsuario.PF ? "529.982.247-25" : "45.997.418/0001-53")
                .build();

        usuario = usuarioRepository.save(usuario);

        Carteira carteira = Carteira.builder()
                .saldo(saldo)
                .usuario(usuario)
                .build();


        carteira = carteiraRepository.save(carteira);
        usuario.setCarteira(carteira);
        return usuarioRepository.save(usuario);
    }

    private RemessaDTO createRemessaDTO(Long usuarioId, BigDecimal valor) {
        return RemessaDTO.builder()
                .usuarioId(usuarioId)
                .valor(valor)
                .moedaDestino("USD")
                .build();
    }

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