package com.guilherme.desafiointer.controller;

import com.guilherme.desafiointer.domain.Remessa;
import com.guilherme.desafiointer.dto.remessa.RemessaRequestDTO;
import com.guilherme.desafiointer.dto.remessa.RemessaResponseDTO;
import com.guilherme.desafiointer.exception.domain.SaldoInsuficienteException;
import com.guilherme.desafiointer.exception.remessa.RemessaException;
import com.guilherme.desafiointer.service.interfaces.RemessaServiceInterface;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST para operações de remessa internacional.
 *
 * Responsável por expor endpoints para realizar transferências internacionais
 * entre usuários PF e PJ, com conversão automática de BRL para USD utilizando
 * cotações oficiais do Banco Central do Brasil.
 *
 * Funcionalidades:
 * - Realização de remessas com validação de limites diários
 * - Conversão automática de moedas (BRL → USD)
 * - Controle de saldo e verificação de carteiras
 * - Aplicação de taxas conforme tipo de usuário
 *
 * Endpoints disponíveis:
 * - POST /api/remessas - Realizar nova remessa
 *
 * Limites diários:
 * - Pessoa Física (PF): R$ 10.000,00
 * - Pessoa Jurídica (PJ): R$ 50.000,00
 *
 * @see RemessaServiceInterface
 * @see RemessaRequestDTO
 * @see RemessaResponseDTO
 */
@RestController
@RequestMapping("/api/remessas")
@RequiredArgsConstructor
@Validated
@Slf4j
public class RemessaController {

    private final RemessaServiceInterface remessaService;

    /**
     * Realiza uma remessa internacional entre usuários.
     *
     * Processa transferência de valores em BRL convertendo para USD
     * utilizando cotação oficial do Banco Central. Aplica validações
     * de saldo, limites diários e taxas conforme tipo de usuário.
     *
     * Validações aplicadas:
     * - Existência dos usuários remetente e destinatário
     * - Saldo suficiente na carteira origem
     * - Limite diário não excedido
     * - Dados válidos da requisição
     *
     * @param request dados da remessa incluindo IDs dos usuários, valor e moeda
     * @return RemessaResponseDTO com detalhes da transação processada
     * @throws RemessaException quando dados inválidos ou regras violadas
     * @throws SaldoInsuficienteException quando saldo insuficiente
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RemessaResponseDTO realizarRemessa(@Valid @RequestBody RemessaRequestDTO request) {
        log.info("Processando remessa internacional: {}", request);
        Remessa remessa = remessaService.realizarRemessa(request);
        return RemessaResponseDTO.from(remessa);
    }
}