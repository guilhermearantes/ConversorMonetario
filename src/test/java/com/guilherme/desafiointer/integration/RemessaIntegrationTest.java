package com.guilherme.desafiointer.integration;

import com.guilherme.desafiointer.domain.*;
import com.guilherme.desafiointer.dto.RemessaDTO;
import com.guilherme.desafiointer.exception.remessa.RemessaException;
import com.guilherme.desafiointer.repository.CarteiraRepository;
import com.guilherme.desafiointer.repository.RemessaRepository;
import com.guilherme.desafiointer.repository.TransacaoDiariaRepository;
import com.guilherme.desafiointer.repository.UsuarioRepository;
import com.guilherme.desafiointer.service.interfaces.RemessaServiceInterface;
import com.guilherme.desafiointer.service.processor.RemessaProcessor;
import com.guilherme.desafiointer.service.validator.RemessaValidator;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Testes de Integração - Remessa")
class RemessaIntegrationTest {

    @Autowired
    private RemessaServiceInterface remessaService;

    @Autowired
    private RemessaProcessor remessaProcessor;

    @Autowired
    private RemessaValidator remessaValidator;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CarteiraRepository carteiraRepository;

    @Autowired
    private RemessaRepository remessaRepository;

    @Autowired
    private TransacaoDiariaRepository transacaoDiariaRepository;

    private Usuario usuarioRemetentePF;
    private Usuario usuarioDestinatarioPF;
    private static final BigDecimal SALDO_INICIAL = new BigDecimal("1000.00");
    private static final String MOEDA_DESTINO = "USD";
    private static final String CPF_REMETENTE = "529.982.247-25";
    private static final String CPF_DESTINATARIO = "248.438.034-80";
    private static final BigDecimal TAXA_PF_PERCENTUAL = new BigDecimal("0.02");

    @BeforeEach
    void setUp() {
        limparDados();
        criarUsuarios();
    }

    private void limparDados() {
        remessaRepository.deleteAll();
        transacaoDiariaRepository.deleteAll();
        carteiraRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    private void criarUsuarios() {
        usuarioRemetentePF = criarUsuario("Remetente PF", "remetente@teste.com",
                CPF_REMETENTE, TipoUsuario.PF);
        usuarioDestinatarioPF = criarUsuario("Destinatário PF", "destinatario@teste.com",
                CPF_DESTINATARIO, TipoUsuario.PF);
    }

    private Usuario criarUsuario(String nome, String email, String documento, TipoUsuario tipo) {
        Usuario usuario = Usuario.builder()
                .nomeCompleto(nome)
                .email(email)
                .senha("Senha@123")
                .tipoUsuario(tipo)
                .documento(documento)
                .build();

        usuario = usuarioRepository.save(usuario);

        Carteira carteira = Carteira.builder()
                .saldo(SALDO_INICIAL)
                .usuario(usuario)
                .build();

        carteira = carteiraRepository.save(carteira);
        usuario.setCarteira(carteira);

        return usuarioRepository.save(usuario);
    }

    @Nested
    @DisplayName("Testes de Validação de Remessa")
    class ValidacaoRemessaTests {

        @Test
        @DisplayName("Deve validar remessa com dados válidos")
        void deveValidarRemessaComDadosValidos() {
            RemessaDTO remessaDTO = criarRemessaDTO(
                    usuarioRemetentePF.getId(),
                    usuarioDestinatarioPF.getId(),
                    new BigDecimal("100.00"));

            assertDoesNotThrow(() -> remessaValidator.validarDadosRemessa(remessaDTO));
        }

        @Test
        @DisplayName("Deve falhar ao validar remessa com dados inválidos")
        void deveFalharAoValidarRemessaComDadosInvalidos() {
            RemessaDTO remessaInvalida = RemessaDTO.builder().build();

            assertThrows(RemessaException.class,
                    () -> remessaValidator.validarDadosRemessa(remessaInvalida),
                    "Deve lançar RemessaException para dados inválidos");
        }
    }

    @Nested
    @DisplayName("Testes de Processamento de Remessa")
    class ProcessamentoRemessaTests {

        @Test
        @Transactional
        @DisplayName("Deve processar remessa com sucesso")
        void deveProcessarRemessaComSucesso() {
            BigDecimal valorRemessa = new BigDecimal("100.00");
            RemessaDTO remessaDTO = criarRemessaDTO(
                    usuarioRemetentePF.getId(),
                    usuarioDestinatarioPF.getId(),
                    valorRemessa);

            Remessa remessa = remessaProcessor.processarRemessa(remessaDTO);

            verificarRemessaProcessada(remessa, valorRemessa);
            verificarSaldoAposRemessa(valorRemessa, remessa.getTaxa());
        }
    }

    @Nested
    @DisplayName("Testes de Integração Completa")
    class IntegracaoCompletaTests {

        @Test
        @Transactional
        @DisplayName("Deve realizar remessa completa com sucesso")
        void deveRealizarRemessaCompletaComSucesso() {
            BigDecimal valorRemessa = new BigDecimal("100.00");
            RemessaDTO remessaDTO = criarRemessaDTO(
                    usuarioRemetentePF.getId(),
                    usuarioDestinatarioPF.getId(),
                    valorRemessa);

            Remessa remessa = remessaService.realizarRemessa(remessaDTO);

            verificarRemessaProcessada(remessa, valorRemessa);
            verificarSaldoAposRemessa(valorRemessa, remessa.getTaxa());
            verificarHistoricoTransacoes(remessa);
        }
    }

    private void verificarRemessaProcessada(Remessa remessa, BigDecimal valorRemessa) {
        assertAll("Validação da remessa processada",
                () -> assertNotNull(remessa.getId(), "ID da remessa deve ser gerado"),
                () -> assertEquals(valorRemessa, remessa.getValor(), "Valor da remessa deve ser mantido"),
                () -> assertEquals(calcularTaxaEsperada(valorRemessa), remessa.getTaxa(),
                        "Taxa deve ser calculada corretamente"),
                () -> assertTrue(remessa.getDataCriacao().isBefore(LocalDateTime.now().plusSeconds(1)),
                        "Data de criação deve ser atual")
        );
    }

    private void verificarSaldoAposRemessa(BigDecimal valorRemessa, BigDecimal taxa) {
        Carteira carteiraAtualizada = carteiraRepository
                .findByUsuarioId(usuarioRemetentePF.getId())
                .orElseThrow(() -> new AssertionError("Carteira não encontrada"));

        BigDecimal valorEsperado = SALDO_INICIAL
                .subtract(valorRemessa)
                .subtract(taxa);

        assertEquals(valorEsperado, carteiraAtualizada.getSaldo(),
                "Saldo deve ser atualizado após a remessa");
    }

    private void verificarHistoricoTransacoes(Remessa remessaRealizada) {
        Page<Remessa> historico = remessaService.buscarHistoricoTransacoes(
                usuarioRemetentePF,
                LocalDateTime.now().minusDays(1),  // Um dia antes
                LocalDateTime.now(),               // Agora (sem adicionar dias)
                PageRequest.of(0, 10)
        );

        assertAll("Validação do histórico",
                () -> assertFalse(historico.isEmpty(), "Histórico não deve estar vazio"),
                () -> assertTrue(historico.getContent().contains(remessaRealizada),
                        "Remessa realizada deve estar no histórico")
        );
    }

    private BigDecimal calcularTaxaEsperada(BigDecimal valor) {
        return valor.multiply(TAXA_PF_PERCENTUAL).setScale(2, RoundingMode.HALF_UP);
    }

    private RemessaDTO criarRemessaDTO(Long remetenteId, Long destinatarioId, BigDecimal valor) {
        return RemessaDTO.builder()
                .usuarioId(remetenteId)
                .destinatarioId(destinatarioId)
                .valor(valor)
                .moedaDestino(MOEDA_DESTINO)
                .build();
    }
}