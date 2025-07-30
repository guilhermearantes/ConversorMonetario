package com.guilherme.desafiointer.integration;

import com.guilherme.desafiointer.config.TestConfig;
import com.guilherme.desafiointer.domain.Carteira;
import com.guilherme.desafiointer.domain.Remessa;
import com.guilherme.desafiointer.domain.TipoUsuario;
import com.guilherme.desafiointer.domain.Usuario;
import com.guilherme.desafiointer.dto.RemessaDTO;
import com.guilherme.desafiointer.exception.domain.LimiteDiarioExcedidoException;
import com.guilherme.desafiointer.exception.domain.SaldoInsuficienteException;
import com.guilherme.desafiointer.exception.remessa.RemessaException;
import com.guilherme.desafiointer.repository.CarteiraRepository;
import com.guilherme.desafiointer.repository.RemessaRepository;
import com.guilherme.desafiointer.repository.TransacaoDiariaRepository;
import com.guilherme.desafiointer.repository.UsuarioRepository;
import com.guilherme.desafiointer.service.interfaces.CotacaoServiceInterface;
import com.guilherme.desafiointer.service.interfaces.RemessaServiceInterface;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@ActiveProfiles("integration-test")
@Import(TestConfig.class)
@DisplayName("Testes de Integração - Remessa")
class RemessaIntegrationTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private RemessaServiceInterface remessaService;

    @Autowired
    private CotacaoServiceInterface cotacaoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CarteiraRepository carteiraRepository;

    @Autowired
    private RemessaRepository remessaRepository;

    @Autowired
    private TransacaoDiariaRepository transacaoDiariaRepository;

    private Usuario usuarioRemetentePF;
    private Usuario usuarioRemetentePJ;
    private Usuario usuarioDestinatarioPF;

    private static final BigDecimal SALDO_INICIAL_PF = new BigDecimal("10000.00");
    private static final BigDecimal SALDO_INICIAL_PJ = new BigDecimal("50000.00");
    private static final String MOEDA_DESTINO = "USD";
    private static final BigDecimal COTACAO_PADRAO = new BigDecimal("5.00");

    @BeforeEach
    void setUp() {
        limparDados();
        criarUsuarios();
    }

    @Nested
    @DisplayName("Testes de Fluxo Principal")
    class FluxoPrincipalTests {

        @Test
        @DisplayName("Deve realizar remessa PF com sucesso")
        void deveRealizarRemessaPFComSucesso() {
            BigDecimal valorRemessa = new BigDecimal("100.00");
            RemessaDTO remessaDTO = criarRemessaDTO(usuarioRemetentePF, valorRemessa);

            Remessa remessa = remessaService.realizarRemessa(remessaDTO);

            BigDecimal taxaEsperada = valorRemessa.multiply(new BigDecimal("0.02"))
                    .setScale(2, RoundingMode.HALF_UP); // 2% de taxa = 2.00

            // Valor em dólares: 100.00 ÷ 5.00 = 20.00
            BigDecimal valorConvertidoEsperado = valorRemessa
                    .divide(new BigDecimal("5.00"), 2, RoundingMode.HALF_UP);

            assertAll("Validação da remessa processada",
                    () -> assertNotNull(remessa.getId()),
                    () -> assertEquals(valorRemessa, remessa.getValor()),
                    () -> assertEquals(taxaEsperada, remessa.getTaxa().setScale(2, RoundingMode.HALF_UP)),
                    () -> assertEquals(valorConvertidoEsperado, remessa.getValorConvertido())
            );

            verificarSaldosAposRemessa(usuarioRemetentePF, usuarioDestinatarioPF,
                    valorRemessa, remessa.getTaxa());
        }

        protected void verificarSaldosAposRemessa(Usuario remetente, Usuario destinatario,
                                                  BigDecimal valorRemessa, BigDecimal taxa) {
            // Verifica saldo do remetente
            Carteira carteiraRemetente = carteiraRepository.findByUsuarioId(remetente.getId())
                    .orElseThrow(() -> new AssertionError("Carteira remetente não encontrada"));

            BigDecimal saldoEsperadoRemetente = SALDO_INICIAL_PF
                    .subtract(valorRemessa)
                    .subtract(taxa);

            // Verifica saldo do destinatário
            Carteira carteiraDestinatario = carteiraRepository.findByUsuarioId(destinatario.getId())
                    .orElseThrow(() -> new AssertionError("Carteira destinatário não encontrada"));

            // Valor em dólares: valorRemessa ÷ 5.00
            BigDecimal valorConvertido = valorRemessa
                    .divide(new BigDecimal("5.00"), 2, RoundingMode.HALF_UP);

            assertAll("Verificação dos saldos",
                    () -> assertEquals(saldoEsperadoRemetente, carteiraRemetente.getSaldo(),
                            "Saldo incorreto para usuário " + remetente.getNomeCompleto()),
                    () -> assertEquals(valorConvertido, carteiraDestinatario.getSaldo(),
                            "Saldo final incorreto para usuário " + destinatario.getNomeCompleto())
            );
        }

        @Test
        @DisplayName("Deve realizar remessa PJ com sucesso")
        void deveRealizarRemessaPJComSucesso() {
            BigDecimal valorRemessa = new BigDecimal("1000.00");
            RemessaDTO remessaDTO = criarRemessaDTO(usuarioRemetentePJ, valorRemessa);

            Remessa remessa = remessaService.realizarRemessa(remessaDTO);

            BigDecimal taxaEsperada = valorRemessa.multiply(new BigDecimal("0.01"))
                    .setScale(2, RoundingMode.HALF_UP); // 1% de taxa para PJ = 10.00

            // Valor em dólares: 1000.00 ÷ 5.00 = 200.00
            BigDecimal valorConvertidoEsperado = valorRemessa
                    .divide(new BigDecimal("5.00"), 2, RoundingMode.HALF_UP);

            assertAll("Validação da remessa processada",
                    () -> assertNotNull(remessa.getId()),
                    () -> assertEquals(valorRemessa, remessa.getValor()),
                    () -> assertEquals(taxaEsperada, remessa.getTaxa().setScale(2, RoundingMode.HALF_UP)),
                    () -> assertEquals(valorConvertidoEsperado, remessa.getValorConvertido())
            );

            verificarSaldosAposRemessaPJ(usuarioRemetentePJ, usuarioDestinatarioPF,
                    valorRemessa, remessa.getTaxa());
        }

        protected void verificarSaldosAposRemessaPJ(Usuario remetente, Usuario destinatario,
                                                    BigDecimal valorRemessa, BigDecimal taxa) {
            // Verifica saldo do remetente
            Carteira carteiraRemetente = carteiraRepository.findByUsuarioId(remetente.getId())
                    .orElseThrow(() -> new AssertionError("Carteira remetente não encontrada"));

            BigDecimal saldoEsperadoRemetente = SALDO_INICIAL_PJ
                    .subtract(valorRemessa)
                    .subtract(taxa);

            // Verifica saldo do destinatário
            Carteira carteiraDestinatario = carteiraRepository.findByUsuarioId(destinatario.getId())
                    .orElseThrow(() -> new AssertionError("Carteira destinatário não encontrada"));

            // Valor em dólares: valorRemessa ÷ 5.00
            BigDecimal valorConvertido = valorRemessa
                    .divide(new BigDecimal("5.00"), 2, RoundingMode.HALF_UP);

            assertAll("Verificação dos saldos",
                    () -> assertEquals(saldoEsperadoRemetente, carteiraRemetente.getSaldo(),
                            "Saldo incorreto para usuário " + remetente.getNomeCompleto()),
                    () -> assertEquals(valorConvertido, carteiraDestinatario.getSaldo(),
                            "Saldo final incorreto para usuário " + destinatario.getNomeCompleto())
            );
        }
    }

    @Nested
    @DisplayName("Testes de Concorrência e Consistência")
    class ConcorrenciaEConsistenciaTests {

        @Test
        @DisplayName("Deve manter consistência em operações concorrentes")
        void deveManterConsistenciaEmOperacoesConcorrentes() throws Exception {
            int numThreads = 5;
            int operacoesPorThread = 2;
            BigDecimal valorPorOperacao = new BigDecimal("100.00");

            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            CountDownLatch latch = new CountDownLatch(numThreads * operacoesPorThread);

            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < numThreads; i++) {
                for (int j = 0; j < operacoesPorThread; j++) {
                    futures.add(executor.submit(() -> {
                        try {
                            remessaService.realizarRemessa(criarRemessaDTO(
                                    usuarioRemetentePF, valorPorOperacao));
                            return null;
                        } finally {
                            latch.countDown();
                        }
                    }));
                }
            }

            latch.await(30, TimeUnit.SECONDS);

            BigDecimal totalOperacoes = valorPorOperacao
                    .multiply(new BigDecimal(numThreads * operacoesPorThread));
            BigDecimal taxaTotal = totalOperacoes
                    .multiply(new BigDecimal("0.02")); // 2% taxa PF

            BigDecimal saldoEsperado = SALDO_INICIAL_PF
                    .subtract(totalOperacoes)
                    .subtract(taxaTotal);

            verificarSaldoFinal(usuarioRemetentePF, saldoEsperado);
        }

        @Test
        @DisplayName("Deve manter consistência em caso de erro")
        @Transactional
        void deveManterConsistenciaEmCasoDeErro() {
            // Primeira remessa com valor que deixará saldo insuficiente para a segunda
            BigDecimal valorPrimeiraRemessa = new BigDecimal("9800.00"); // Deixará ~R$200 de saldo após taxa
            remessaService.realizarRemessa(criarRemessaDTO(usuarioRemetentePF, valorPrimeiraRemessa));

            // Força flush e refresh da entidade
            entityManager.flush();
            entityManager.refresh(carteiraRepository.findByUsuarioId(usuarioRemetentePF.getId()).get());

            // Segunda remessa deve falhar por saldo insuficiente (valor + taxa > saldo restante)
            BigDecimal valorSegundaRemessa = new BigDecimal("200.00");
            assertThrows(SaldoInsuficienteException.class, () ->
                    remessaService.realizarRemessa(criarRemessaDTO(usuarioRemetentePF, valorSegundaRemessa)));

            // Verifica se o saldo foi mantido consistente após o erro
            BigDecimal taxaPrimeiraRemessa = valorPrimeiraRemessa.multiply(new BigDecimal("0.02"));
            BigDecimal saldoEsperado = SALDO_INICIAL_PF
                    .subtract(valorPrimeiraRemessa)
                    .subtract(taxaPrimeiraRemessa);

            verificarSaldoFinal(usuarioRemetentePF, saldoEsperado);
        }
    }

    @Nested
    @DisplayName("Testes de Regras de Negócio")
    class RegrasNegocioTests {

        @Test
        @DisplayName("Deve respeitar limite diário de PF")
        void deveRespeitarLimiteDiarioPF() {
            // Primeira remessa (dentro do limite)
            assertDoesNotThrow(() ->
                    remessaService.realizarRemessa(criarRemessaDTO(
                            usuarioRemetentePF, new BigDecimal("9000.00"))));

            // Segunda remessa (deve estourar o limite)
            assertThrows(LimiteDiarioExcedidoException.class, () ->
                    remessaService.realizarRemessa(criarRemessaDTO(
                            usuarioRemetentePF, new BigDecimal("2000.00"))));
        }

        @Test
        @DisplayName("Deve respeitar limite diário de PJ")
        void deveRespeitarLimiteDiarioPJ() {
            // Primeira remessa (dentro do limite)
            assertDoesNotThrow(() ->
                    remessaService.realizarRemessa(criarRemessaDTO(
                            usuarioRemetentePJ, new BigDecimal("45000.00"))));

            // Segunda remessa (deve estourar o limite)
            assertThrows(LimiteDiarioExcedidoException.class, () ->
                    remessaService.realizarRemessa(criarRemessaDTO(
                            usuarioRemetentePJ, new BigDecimal("10000.00"))));
        }

        @Test
        @DisplayName("Deve usar cotação de fim de semana")
        void deveUsarCotacaoFimDeSemana() {
            // Implementar quando tivermos acesso ao serviço de cotação
            // Este teste depende da implementação específica do serviço de cotação
        }
    }

    // Métodos auxiliares
    private void limparDados() {
        remessaRepository.deleteAllInBatch();
        transacaoDiariaRepository.deleteAllInBatch();
        carteiraRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();
    }


    private void criarUsuarios() {
        usuarioRemetentePF = criarUsuario("Remetente PF", "remetente.pf@teste.com",
                "529.982.247-25", TipoUsuario.PF, SALDO_INICIAL_PF);
        usuarioRemetentePJ = criarUsuario("Remetente PJ", "remetente.pj@teste.com",
                "45.997.418/0001-53", TipoUsuario.PJ, SALDO_INICIAL_PJ); // CNPJ válido usado em outros testes
        usuarioDestinatarioPF = criarUsuario("Destinatário PF", "destinatario@teste.com",
                "248.438.034-80", TipoUsuario.PF, BigDecimal.ZERO);
    }

    private Usuario criarUsuario(String nome, String email, String documento,
                                 TipoUsuario tipo, BigDecimal saldoInicial) {
        Usuario usuario = Usuario.builder()
                .nomeCompleto(nome)
                .email(email)
                .senha("Senha@123")
                .tipoUsuario(tipo)
                .documento(documento)
                .build();

        usuario = usuarioRepository.save(usuario);

        Carteira carteira = Carteira.builder()
                .saldo(saldoInicial)
                .usuario(usuario)
                .build();

        carteira = carteiraRepository.save(carteira);
        usuario.setCarteira(carteira);

        return usuarioRepository.save(usuario);
    }

    private RemessaDTO criarRemessaDTO(Usuario remetente, BigDecimal valor) {
        return RemessaDTO.builder()
                .usuarioId(remetente.getId())
                .destinatarioId(usuarioDestinatarioPF.getId())
                .valor(valor)
                .moedaDestino(MOEDA_DESTINO)
                .build();
    }

    private void verificarRemessaProcessada(Remessa remessa, BigDecimal valorRemessa,
                                            BigDecimal taxaPercentual) {
        assertAll("Validação da remessa processada",
                () -> assertNotNull(remessa.getId()),
                () -> assertEquals(valorRemessa, remessa.getValor()),
                () -> assertEquals(
                        valorRemessa.multiply(taxaPercentual).setScale(2, RoundingMode.HALF_UP),
                        remessa.getTaxa()),
                () -> assertEquals(COTACAO_PADRAO, remessa.getCotacao()),
                () -> assertTrue(remessa.getDataCriacao().isBefore(LocalDateTime.now().plusSeconds(1)))
        );
    }

    private void verificarSaldosAposRemessa(Usuario remetente, Usuario destinatario,
                                            BigDecimal valorRemessa, BigDecimal taxa) {
        BigDecimal saldoRemetenteEsperado = remetente.getCarteira().getSaldo()
                .subtract(valorRemessa)
                .subtract(taxa);

        BigDecimal saldoDestinatarioEsperado = destinatario.getCarteira().getSaldo()
                .add(valorRemessa.multiply(COTACAO_PADRAO));

        verificarSaldoFinal(remetente, saldoRemetenteEsperado);
        verificarSaldoFinal(destinatario, saldoDestinatarioEsperado);
    }

    private void verificarSaldoFinal(Usuario usuario, BigDecimal saldoEsperado) {
        BigDecimal saldoAtual = carteiraRepository
                .findByUsuarioId(usuario.getId())
                .get()
                .getSaldo();
        assertEquals(saldoEsperado.setScale(2, RoundingMode.HALF_UP),
                saldoAtual.setScale(2, RoundingMode.HALF_UP),
                "Saldo final incorreto para usuário " + usuario.getNomeCompleto());
    }
}