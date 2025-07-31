package com.guilherme.desafiointer.test;

import com.guilherme.desafiointer.domain.*;
import com.guilherme.desafiointer.dto.remessa.RemessaRequestDTO;
import com.guilherme.desafiointer.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

// Definir como um Spring Bean genérico
@Component
public abstract class TestBase {

    protected static final BigDecimal SALDO_PADRAO_BRL = BigDecimal.valueOf(1000.00);
    protected static final BigDecimal SALDO_PADRAO_USD = BigDecimal.valueOf(500.00);
    protected static final String SENHA_PADRAO = "Senha@123";

    private static final String DISABLE_REFERENTIAL_INTEGRITY = "SET REFERENTIAL_INTEGRITY FALSE";
    private static final String ENABLE_REFERENTIAL_INTEGRITY = "SET REFERENTIAL_INTEGRITY TRUE";
    private static final String TRUNCATE_REMESSAS = "TRUNCATE TABLE remessas";
    private static final String TRUNCATE_TRANSACOES_DIARIAS = "TRUNCATE TABLE transacoes_diarias";
    private static final String TRUNCATE_CARTEIRAS = "TRUNCATE TABLE carteiras";
    private static final String TRUNCATE_USUARIOS = "TRUNCATE TABLE usuarios";

    @PersistenceContext
    protected EntityManager entityManager;

    protected final UsuarioRepository usuarioRepository;
    protected final CarteiraRepository carteiraRepository;
    protected final RemessaRepository remessaRepository;
    protected final TransacaoDiariaRepository transacaoDiariaRepository;

    @Autowired
    public TestBase(UsuarioRepository usuarioRepository,
                    CarteiraRepository carteiraRepository,
                    RemessaRepository remessaRepository,
                    TransacaoDiariaRepository transacaoDiariaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.carteiraRepository = carteiraRepository;
        this.remessaRepository = remessaRepository;
        this.transacaoDiariaRepository = transacaoDiariaRepository;
    }

    @Transactional
    protected void limparBancoDeDados() {
        entityManager.createNativeQuery(DISABLE_REFERENTIAL_INTEGRITY).executeUpdate();
        entityManager.createNativeQuery(TRUNCATE_REMESSAS).executeUpdate();
        entityManager.createNativeQuery(TRUNCATE_TRANSACOES_DIARIAS).executeUpdate();
        entityManager.createNativeQuery(TRUNCATE_CARTEIRAS).executeUpdate();
        entityManager.createNativeQuery(TRUNCATE_USUARIOS).executeUpdate();
        entityManager.createNativeQuery(ENABLE_REFERENTIAL_INTEGRITY).executeUpdate();
        entityManager.flush();
    }

    protected Usuario salvarUsuarioComCarteira(final String nome, final String email, final String documento,
                                               final TipoUsuario tipo, final BigDecimal saldoBRL, final BigDecimal saldoUSD) {
        final Usuario usuario = criarUsuario(nome, email, documento, tipo);
        final Carteira carteira = criarCarteira(usuario, saldoBRL, saldoUSD);

        usuario.setCarteira(carteiraRepository.save(carteira));
        return usuarioRepository.save(usuario);
    }

    protected Usuario salvarUsuarioComCarteiraPadrao(final String nome, final String email,
                                                     final String documento, final TipoUsuario tipo) {
        return salvarUsuarioComCarteira(nome, email, documento, tipo, SALDO_PADRAO_BRL, SALDO_PADRAO_USD);
    }

    protected RemessaRequestDTO criarRemessaDTO(final Long usuarioId, final Long destinatarioId,
                                                final BigDecimal valor, final String moedaDestino) {
        return RemessaRequestDTO.builder()
                .usuarioId(usuarioId)
                .destinatarioId(destinatarioId)
                .valor(valor)
                .moedaDestino(moedaDestino)
                .build();
    }

    private Usuario criarUsuario(final String nome, final String email, final String documento, final TipoUsuario tipo) {
        return usuarioRepository.save(Usuario.builder()
                .nomeCompleto(nome)
                .email(email)
                .documento(documento)
                .tipoUsuario(tipo)
                .senha(SENHA_PADRAO)
                .build());
    }

    private Carteira criarCarteira(final Usuario usuario, final BigDecimal saldoBRL, final BigDecimal saldoUSD) {
        return Carteira.builder()
                .usuario(usuario)
                .saldoBRL(saldoBRL)
                .saldoUSD(saldoUSD)
                .build();
    }
}