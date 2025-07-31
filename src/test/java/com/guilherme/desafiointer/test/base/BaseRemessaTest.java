package com.guilherme.desafiointer.test.base;

import com.guilherme.desafiointer.domain.Remessa;
import com.guilherme.desafiointer.domain.TipoUsuario;
import com.guilherme.desafiointer.domain.Usuario;
import com.guilherme.desafiointer.dto.remessa.RemessaRequestDTO;
import com.guilherme.desafiointer.repository.CarteiraRepository;
import com.guilherme.desafiointer.repository.RemessaRepository;
import com.guilherme.desafiointer.repository.TransacaoDiariaRepository;
import com.guilherme.desafiointer.repository.UsuarioRepository;
import com.guilherme.desafiointer.test.TestBase;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public abstract class BaseRemessaTest extends TestBase {

    protected Usuario usuarioRemetente;
    protected Usuario usuarioDestinatario;

    @Autowired
    public BaseRemessaTest(UsuarioRepository usuarioRepository,
                           CarteiraRepository carteiraRepository,
                           RemessaRepository remessaRepository,
                           TransacaoDiariaRepository transacaoDiariaRepository) {
        super(usuarioRepository, carteiraRepository, remessaRepository, transacaoDiariaRepository);
    }

    @BeforeEach
    protected void setUpBase() {
        limparBancoDeDados();
        configurarUsuarios();
    }

    private void configurarUsuarios() {
        // Criar usuário remetente com saldo inicial em BRL e USD
        usuarioRemetente = salvarUsuarioComCarteira(
                "João Remetente",
                "remetente@email.com",
                "123.456.789-00",
                TipoUsuario.PF,
                new BigDecimal("1000.00"), // saldoBRL
                new BigDecimal("500.00")   // saldoUSD
        );

        // Criar usuário destinatário com saldo inicial em BRL e USD
        usuarioDestinatario = salvarUsuarioComCarteira(
                "Maria Destinatária",
                "destinataria@email.com",
                "987.654.321-00",
                TipoUsuario.PF,
                new BigDecimal("2000.00"), // saldoBRL
                new BigDecimal("1000.00")  // saldoUSD
        );
    }

    /**
     * Método auxiliar para calcular a taxa esperada com base em um valor e percentual fornecido.
     *
     * @param valor      Valor da remessa.
     * @param percentual Percentual da taxa.
     * @return Taxa calculada.
     */
    protected BigDecimal calcularTaxaEsperada(BigDecimal valor, BigDecimal percentual) {
        return valor.multiply(percentual).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Verifica se o histórico de transações inclui a remessa realizada.
     *
     * @param remessaRealizada A remessa que deve estar no histórico.
     */
    protected void verificarHistoricoTransacoes(Remessa remessaRealizada) {
        Page<Remessa> historico = remessaRepository.buscarHistoricoTransacoes(
                usuarioRemetente,
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

    /**
     * Cria um DTO para teste de remessa.
     *
     * @param usuarioId       ID do usuário remetente.
     * @param destinatarioId  ID do usuário destinatário.
     * @param valor           Valor da remessa a ser transferido.
     * @param moedaDestino    Moeda desejada para conversão.
     * @return Um objeto RemessaRequestDTO configurado.
     */
    protected RemessaRequestDTO criarRemessaDTO(final Long usuarioId, final Long destinatarioId,
                                                final BigDecimal valor, final String moedaDestino) {
        return RemessaRequestDTO.builder()
                .usuarioId(usuarioId)
                .destinatarioId(destinatarioId)
                .valor(valor)
                .moedaDestino(moedaDestino)
                .build();
    }
}