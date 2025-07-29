package com.guilherme.desafiointer.test.base;

import com.guilherme.desafiointer.domain.*;
import com.guilherme.desafiointer.dto.RemessaDTO;
import com.guilherme.desafiointer.test.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseRemessaTest extends TestBase {
    // Constantes específicas de Remessa
    protected static final String MOEDA_DESTINO = "USD";
    protected static final BigDecimal VALOR_REMESSA_PADRAO = new BigDecimal("100.00");
    protected static final BigDecimal TAXA_PF_PERCENTUAL = new BigDecimal("0.02");

    // Dados do Remetente
    protected static final String NOME_REMETENTE = "Remetente PF";
    protected static final String EMAIL_REMETENTE = "remetente@teste.com";
    protected static final String CPF_REMETENTE = "529.982.247-25";

    // Dados do Destinatário
    protected static final String NOME_DESTINATARIO = "Destinatário PF";
    protected static final String EMAIL_DESTINATARIO = "destinatario@teste.com";
    protected static final String CPF_DESTINATARIO = "248.438.034-80";

    // Atributos protegidos para testes
    protected Usuario usuarioRemetentePF;
    protected Usuario usuarioDestinatarioPF;

    @BeforeEach
    protected void setUpBase() {
        limparBancoDados();
        criarUsuarios();
    }

    protected void criarUsuarios() {
        usuarioRemetentePF = criarEPersistirUsuario(NOME_REMETENTE, EMAIL_REMETENTE,
                CPF_REMETENTE, TipoUsuario.PF);
        usuarioDestinatarioPF = criarEPersistirUsuario(NOME_DESTINATARIO, EMAIL_DESTINATARIO,
                CPF_DESTINATARIO, TipoUsuario.PF);
    }

    protected void verificarRemessaProcessada(Remessa remessa, BigDecimal valorRemessa) {
        assertAll("Validação da remessa processada",
                () -> assertNotNull(remessa.getId(), "ID da remessa deve ser gerado"),
                () -> assertEquals(valorRemessa, remessa.getValor(), "Valor da remessa deve ser mantido"),
                () -> assertEquals(calcularTaxaEsperada(valorRemessa), remessa.getTaxa(),
                        "Taxa deve ser calculada corretamente"),
                () -> assertTrue(remessa.getDataCriacao().isBefore(LocalDateTime.now().plusSeconds(1)),
                        "Data de criação deve ser atual")
        );
    }

    protected void verificarSaldoAposRemessa(BigDecimal valorRemessa, BigDecimal taxa) {
        Carteira carteiraAtualizada = carteiraRepository
                .findByUsuarioId(usuarioRemetentePF.getId())
                .orElseThrow(() -> new AssertionError("Carteira não encontrada"));

        BigDecimal valorEsperado = SALDO_PADRAO
                .subtract(valorRemessa)
                .subtract(taxa);

        assertEquals(valorEsperado, carteiraAtualizada.getSaldo(),
                "Saldo deve ser atualizado após a remessa");
    }

    protected void verificarHistoricoTransacoes(Remessa remessaRealizada) {
        Page<Remessa> historico = remessaRepository.buscarHistoricoTransacoes(
                usuarioRemetentePF,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now(),
                org.springframework.data.domain.PageRequest.of(0, 10)
        );

        assertAll("Validação do histórico",
                () -> assertFalse(historico.isEmpty(), "Histórico não deve estar vazio"),
                () -> assertTrue(historico.getContent().contains(remessaRealizada),
                        "Remessa realizada deve estar no histórico")
        );
    }

    protected BigDecimal calcularTaxaEsperada(BigDecimal valor) {
        return valor.multiply(TAXA_PF_PERCENTUAL).setScale(2, RoundingMode.HALF_UP);
    }

    protected RemessaDTO criarRemessaDTO(Long remetenteId, Long destinatarioId, BigDecimal valor) {
        return super.criarRemessaDTO(remetenteId, destinatarioId, valor, MOEDA_DESTINO);
    }
}