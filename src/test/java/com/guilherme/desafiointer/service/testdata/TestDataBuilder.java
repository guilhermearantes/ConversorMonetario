package com.guilherme.desafiointer.service.testdata;

import com.guilherme.desafiointer.config.constants.AppConstants;
import com.guilherme.desafiointer.domain.CotacaoHistorico;
import com.guilherme.desafiointer.domain.Remessa;
import com.guilherme.desafiointer.domain.TipoUsuario;
import com.guilherme.desafiointer.domain.Usuario;
import com.guilherme.desafiointer.dto.integration.bcb.PTAXResponse;
import com.guilherme.desafiointer.dto.remessa.RemessaRequestDTO;
import com.guilherme.desafiointer.exception.remessa.RemessaErrorType;
import com.guilherme.desafiointer.exception.remessa.RemessaException;
import lombok.experimental.UtilityClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@UtilityClass
public class TestDataBuilder {
    // Constantes monetárias
    public static final BigDecimal COTACAO_PADRAO = new BigDecimal("5.0000");
    public static final String MOEDA_PADRAO = "USD";
    public static final BigDecimal VALOR_REMESSA_PADRAO = new BigDecimal("100.00");
    public static final BigDecimal TAXA_PERCENTUAL_PF = new BigDecimal("0.02");

    // Constantes de usuários
    private static final String NOME_REMETENTE = "Remetente PF";
    private static final String EMAIL_REMETENTE = "remetente@teste.com";
    private static final String CPF_REMETENTE = "529.982.247-25";
    private static final String NOME_DESTINATARIO = "Destinatário PF";
    private static final String EMAIL_DESTINATARIO = "destinatario@teste.com";
    private static final String CPF_DESTINATARIO = "248.438.034-80";

    // Constantes de mensagens de erro
    public static final String MENSAGEM_DADOS_INVALIDOS = "Dados inválidos para a operação: Dados inválidos";
    public static final String MENSAGEM_USUARIO_NAO_ENCONTRADO = "Usuário não encontrado";
    public static final String MENSAGEM_CARTEIRA_NAO_ENCONTRADA = "Carteira não encontrada";
    public static final String MENSAGEM_SALDO_INSUFICIENTE = "Saldo insuficiente";

    public static PTAXResponse criarPTAXResponseVazio() {
        PTAXResponse response = new PTAXResponse();
        response.setValue(Collections.emptyList());
        return response;
    }

    public static PTAXResponse criarPTAXResponsePadrao() {
        PTAXResponse response = new PTAXResponse();
        PTAXResponse.PTAXValue value = new PTAXResponse.PTAXValue();
        value.setCotacaoCompra(COTACAO_PADRAO.toString());
        response.setValue(Collections.singletonList(value));
        return response;
    }

    public static CotacaoHistorico criarCotacaoHistorico(String moeda, BigDecimal valor, boolean isFimDeSemana) {
        return criarCotacaoHistorico(moeda, valor, isFimDeSemana, LocalDateTime.now());
    }

    public static CotacaoHistorico criarCotacaoHistorico(String moeda, BigDecimal valor,
                                                         boolean isFimDeSemana, LocalDateTime dataHora) {
        return CotacaoHistorico.builder()
                .id(1L)
                .moeda(moeda)
                .valor(valor)
                .dataHora(dataHora)
                .isFimDeSemana(isFimDeSemana)
                .ultimaAtualizacao(LocalDateTime.now())
                .build();
    }


    public static Usuario criarRemetentePadrao() {
        return criarUsuario(1L, NOME_REMETENTE, EMAIL_REMETENTE, CPF_REMETENTE, TipoUsuario.PF);
    }

    public static Usuario criarDestinatarioPadrao() {
        return criarUsuario(2L, NOME_DESTINATARIO, EMAIL_DESTINATARIO, CPF_DESTINATARIO, TipoUsuario.PF);
    }

    public static RemessaRequestDTO criarRemessaPadrao(Long remetenteId, Long destinatarioId) {
        return RemessaRequestDTO.builder()
                .usuarioId(remetenteId)
                .destinatarioId(destinatarioId)
                .valor(VALOR_REMESSA_PADRAO)
                .moedaDestino(AppConstants.MOEDA_PADRAO)
                .build();
    }

    public static Usuario criarUsuario(Long id, String nome, String email, String documento, TipoUsuario tipo) {
        return Usuario.builder()
                .id(id)
                .nomeCompleto(nome)
                .email(email)
                .documento(documento)
                .tipoUsuario(tipo)
                .build();
    }

    public static Remessa criarRemessaProcessada(RemessaRequestDTO dto, Usuario remetente, Usuario destinatario) {
        return Remessa.builder()
                .id(1L)
                .usuario(remetente)
                .destinatario(destinatario)
                .valor(dto.getValor().setScale(2, RoundingMode.HALF_UP))
                .valorConvertido(dto.getValor()
                        .divide(COTACAO_PADRAO, 2, RoundingMode.HALF_UP))
                .taxa(dto.getValor()
                        .multiply(TAXA_PERCENTUAL_PF)
                        .setScale(2, RoundingMode.HALF_UP))
                .moedaDestino(dto.getMoedaDestino())
                .cotacao(COTACAO_PADRAO.setScale(2, RoundingMode.HALF_UP))
                .dataCriacao(LocalDateTime.now())
                .build();
    }

    public static Page<Remessa> criarPaginaHistorico(RemessaRequestDTO dto, Usuario remetente, Usuario destinatario) {
        return new PageImpl<>(List.of(criarRemessaProcessada(dto, remetente, destinatario)));
    }

    public static RemessaException criarExcecaoValidacao() {
        return RemessaException.validacao(
                RemessaErrorType.DADOS_INVALIDOS,
                MENSAGEM_DADOS_INVALIDOS
        );
    }

    public static RemessaException criarExcecaoUsuarioNaoEncontrado() {
        return RemessaException.negocio(
                RemessaErrorType.USUARIO_NAO_ENCONTRADO,
                MENSAGEM_USUARIO_NAO_ENCONTRADO
        );
    }

    public static RemessaException criarExcecaoCarteiraNaoEncontrada() {
        return RemessaException.negocio(
                RemessaErrorType.CARTEIRA_NAO_ENCONTRADA,
                MENSAGEM_CARTEIRA_NAO_ENCONTRADA
        );
    }
}