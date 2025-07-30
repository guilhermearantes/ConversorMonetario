package com.guilherme.desafiointer.test;

import com.guilherme.desafiointer.domain.*;
import com.guilherme.desafiointer.dto.remessa.RemessaRequestDTO;
import com.guilherme.desafiointer.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

public abstract class TestBase {
    // Constantes básicas
    protected static final BigDecimal SALDO_PADRAO = new BigDecimal("1000.00");
    protected static final String SENHA_PADRAO = "Senha@123";

    // Injeções de dependências comuns
    @PersistenceContext
    protected EntityManager entityManager;

    @Autowired
    protected UsuarioRepository usuarioRepository;

    @Autowired
    protected CarteiraRepository carteiraRepository;

    @Autowired
    protected RemessaRepository remessaRepository;

    @Autowired
    protected TransacaoDiariaRepository transacaoDiariaRepository;

    // Métodos utilitários base
    @Transactional
    protected void limparBancoDados() {
        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE remessas").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE transacoes_diarias").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE carteiras").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE usuarios").executeUpdate();
        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
        entityManager.flush();
    }

    protected Usuario criarEPersistirUsuario(String nome, String email,
                                             String documento, TipoUsuario tipo) {
        Usuario usuario = Usuario.builder()
                .nomeCompleto(nome)
                .email(email)
                .documento(documento)
                .tipoUsuario(tipo)
                .senha(SENHA_PADRAO)
                .build();

        usuario = usuarioRepository.save(usuario);

        Carteira carteira = Carteira.builder()
                .saldo(SALDO_PADRAO)
                .usuario(usuario)
                .build();

        carteira = carteiraRepository.save(carteira);
        usuario.setCarteira(carteira);

        return usuarioRepository.save(usuario);
    }

    protected RemessaRequestDTO criarRemessaDTO(Long usuarioId, Long destinatarioId,
                                                BigDecimal valor, String moedaDestino) {
        return RemessaRequestDTO.builder()
                .usuarioId(usuarioId)
                .destinatarioId(destinatarioId)
                .valor(valor)
                .moedaDestino(moedaDestino)
                .build();
    }
}