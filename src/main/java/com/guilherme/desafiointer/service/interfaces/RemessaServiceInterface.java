package com.guilherme.desafiointer.service.interfaces;

import com.guilherme.desafiointer.domain.Remessa;
import com.guilherme.desafiointer.domain.Usuario;
import com.guilherme.desafiointer.dto.remessa.RemessaRequestDTO;
import com.guilherme.desafiointer.exception.domain.LimiteDiarioExcedidoException;
import com.guilherme.desafiointer.exception.domain.SaldoInsuficienteException;
import com.guilherme.desafiointer.exception.remessa.RemessaException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

/**
 * Interface que define as operações disponíveis para o serviço de remessas.
 */
public interface RemessaServiceInterface {
    /**
     * Realiza uma remessa internacional entre usuarios.
     *
     * @param remessaRequestDTO DTO contendo os dados da remessa
     * @return Remessa processada e salva
     * @throws IllegalArgumentException se dados obrigatórios estiverem ausentes
     * @throws SaldoInsuficienteException se o saldo for insuficiente
     * @throws RemessaException com RemessaErrorType.CARTEIRA_NAO_ENCONTRADA se alguma carteira não for encontrada
     * @throws LimiteDiarioExcedidoException se o limite diário for excedido
     */
    Remessa realizarRemessa(@Valid RemessaRequestDTO remessaRequestDTO);

    /**
     * Busca o histórico de transações de um usuario num período específico.
     *
     * @param usuario usuário alvo da busca
     * @param inicio início do período
     * @param fim fim do período
     * @param pageable configuração de paginação
     * @return Page<Remessa> contendo as remessas do período
     * @throws IllegalArgumentException se algum parâmetro obrigatório for nulo
     */
    Page<Remessa> buscarHistoricoTransacoes(Usuario usuario, LocalDateTime inicio, LocalDateTime fim, Pageable pageable);
}