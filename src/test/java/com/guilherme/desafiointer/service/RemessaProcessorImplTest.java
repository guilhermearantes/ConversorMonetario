package com.guilherme.desafiointer.service;

import com.guilherme.desafiointer.config.TestConfig;
import com.guilherme.desafiointer.domain.*;
import com.guilherme.desafiointer.dto.remessa.RemessaRequestDTO;
import com.guilherme.desafiointer.repository.CarteiraRepository;
import com.guilherme.desafiointer.repository.RemessaRepository;
import com.guilherme.desafiointer.repository.TransacaoDiariaRepository;
import com.guilherme.desafiointer.service.interfaces.CotacaoServiceInterface;
import com.guilherme.desafiointer.service.processor.RemessaProcessorImpl;
import com.guilherme.desafiointer.service.strategy.LimiteDiarioValidator;
import com.guilherme.desafiointer.service.strategy.StrategyFactory;
import com.guilherme.desafiointer.service.strategy.TaxaStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(TestConfig.class)
@DisplayName("Testes do RemessaProcessor")
class RemessaProcessorImplTest {

    @Autowired
    private RemessaProcessorImpl remessaProcessor;

    @MockBean
    private CotacaoServiceInterface cotacaoService;
    @MockBean
    private RemessaRepository remessaRepository;
    @MockBean
    private CarteiraRepository carteiraRepository;
    @MockBean
    private TransacaoDiariaRepository transacaoDiariaRepository;
    @MockBean
    private StrategyFactory strategyFactory;
    @MockBean
    private TaxaStrategy taxaStrategy;
    @MockBean
    private LimiteDiarioValidator limiteDiarioValidator;

    private static final BigDecimal SALDO_INICIAL = new BigDecimal("1000.00");
    private static final BigDecimal VALOR_REMESSA = new BigDecimal("100.00");
    private static final BigDecimal TAXA = new BigDecimal("2.00");
    private static final BigDecimal COTACAO = new BigDecimal("5.00");
    private static final String MOEDA_DESTINO = "USD";

    private Usuario usuarioRemetente;
    private Usuario usuarioDestinatario;
    private Carteira carteiraRemetente;
    private Carteira carteiraDestinatario;
    private RemessaRequestDTO remessaRequestDTO;

    @BeforeEach
    void setUp() {
        // Criar usuários de teste
        usuarioRemetente = criarUsuario(1L, "Remetente");
        usuarioDestinatario = criarUsuario(2L, "Destinatario");

        // Criar carteiras de teste com saldos BRL e USD
        carteiraRemetente = criarCarteira(usuarioRemetente, new BigDecimal("1000.00"), new BigDecimal("500.00"));
        carteiraDestinatario = criarCarteira(usuarioDestinatario, new BigDecimal("2000.00"), new BigDecimal("1000.00"));

        // Criar DTO de remessa
        remessaRequestDTO = criarRemessaDTO();

        // Mockar comportamentos para repositórios e serviços dependentes
        when(carteiraRepository.findByUsuarioIdWithPessimisticLock(usuarioRemetente.getId()))
                .thenReturn(Optional.of(carteiraRemetente));
        when(carteiraRepository.findByUsuarioIdWithPessimisticLock(usuarioDestinatario.getId()))
                .thenReturn(Optional.of(carteiraDestinatario));
        when(cotacaoService.obterCotacao(anyString())).thenReturn(new BigDecimal("5.00"));
    }

    // Métodos auxiliares
    private Usuario criarUsuario(Long id, String nome) {
        return Usuario.builder()
                .id(id)
                .nomeCompleto(nome)
                .email(nome.toLowerCase() + "@teste.com")
                .tipoUsuario(TipoUsuario.PF)
                .documento("123.456.789-00")
                .build();
    }

    private Carteira criarCarteira(Usuario usuario, BigDecimal saldoBRL, BigDecimal saldoUSD) {
        return Carteira.builder()
                .id(usuario.getId())
                .usuario(usuario)
                .saldoBRL(saldoBRL)
                .saldoUSD(saldoUSD)
                .build();
    }

    private RemessaRequestDTO criarRemessaDTO() {
        return RemessaRequestDTO.builder()
                .usuarioId(1L)
                .destinatarioId(2L)
                .valor(VALOR_REMESSA)
                .moedaDestino(MOEDA_DESTINO)
                .build();
    }
}