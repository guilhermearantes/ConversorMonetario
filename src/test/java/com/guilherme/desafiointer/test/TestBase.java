package com.guilherme.desafiointer.service;

import com.guilherme.desafiointer.domain.Carteira;
import com.guilherme.desafiointer.domain.TipoUsuario;
import com.guilherme.desafiointer.domain.Usuario;
import com.guilherme.desafiointer.dto.RemessaDTO;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class TestBase {

    @PersistenceContext
    protected EntityManager entityManager;

    // Constantes úteis para testes
    protected static final String CPF_VALIDO = "52998224725";
    protected static final String SENHA_PADRAO = "senha123";
    protected static final String NOME_PADRAO = "João Silva";
    protected static final String EMAIL_PADRAO = "joao@email.com";
    protected static final BigDecimal SALDO_PADRAO = new BigDecimal("1000.00");

    private static final List<String> TABELAS_SISTEMA = Arrays.asList(
            "remessas",
            "transacoes_diarias",
            "carteiras",
            "usuarios"
    );

    @PostConstruct
    public void init() {
        entityManager.setFlushMode(FlushModeType.COMMIT);
    }

    protected void limparBancoDados() {
        entityManager.flush();
        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();

        for (String tabela : TABELAS_SISTEMA) {
            entityManager.createNativeQuery("TRUNCATE TABLE " + tabela).executeUpdate();
        }

        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    @Transactional
    protected Usuario criarEPersistirUsuarioPF(String nome, String email, String cpf, BigDecimal saldoInicial) {
        Usuario usuario = Usuario.builder()
                .nomeCompleto(nome)
                .email(email)
                .documento(cpf)
                .senha(SENHA_PADRAO)
                .tipoUsuario(TipoUsuario.PF)
                .build();

        Carteira carteira = Carteira.builder()
                .usuario(usuario)
                .saldo(saldoInicial)
                .build();

        usuario.setCarteira(carteira);

        entityManager.persist(usuario);
        entityManager.persist(carteira);
        entityManager.flush();

        return usuario;
    }

    @Transactional
    protected Usuario criarEPersistirUsuarioPF() {
        return criarEPersistirUsuarioPF(NOME_PADRAO, EMAIL_PADRAO, CPF_VALIDO, SALDO_PADRAO);
    }

    protected RemessaDTO criarRemessaDTO(Long usuarioId, BigDecimal valor, String moedaDestino) {
        return RemessaDTO.builder()
                .usuarioId(usuarioId)
                .valor(valor)
                .moedaDestino(moedaDestino)
                .build();
    }
}