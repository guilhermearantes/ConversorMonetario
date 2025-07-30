package com.guilherme.desafiointer.service.validator;

import com.guilherme.desafiointer.domain.Usuario;
import com.guilherme.desafiointer.dto.remessa.RemessaRequestDTO;
import com.guilherme.desafiointer.exception.remessa.RemessaErrorType;
import com.guilherme.desafiointer.exception.remessa.RemessaException;
import com.guilherme.desafiointer.repository.UsuarioRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * Componente responsável pela validação de dados relacionados a remessas.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RemessaValidator {

    private static final int PERIODO_MAXIMO_DIAS = 90;
    private static final int TAMANHO_MAXIMO_PAGINA = 50;

    private final Validator validator;
    private final UsuarioRepository usuarioRepository;

    /**
     * Valida os dados da requisição de remessa.
     *
     * @param remessaRequestDTO DTO contendo os dados da remessa
     * @throws RemessaException se houver erro na validação
     */
    public void validarDadosRemessa(RemessaRequestDTO remessaRequestDTO) {
        validarCamposObrigatorios(remessaRequestDTO);
        validarUsuarios(remessaRequestDTO);
        validarRemetenteDestinatario(remessaRequestDTO);
    }

    /**
     * Valida os parâmetros de busca do histórico de remessas.
     *
     * @param usuario usuário que está realizando a consulta
     * @param inicio data inicial do período de consulta
     * @param fim data final do período de consulta
     * @param pageable informações de paginação
     * @throws RemessaException se houver erro na validação
     */
    public void validarParametrosBusca(Usuario usuario, LocalDateTime inicio, LocalDateTime fim, Pageable pageable) {
        if (usuario == null) {
            throw RemessaException.validacao(
                    RemessaErrorType.DADOS_INVALIDOS,
                    "Usuário é obrigatório para consulta"
            );
        }

        if (inicio == null || fim == null) {
            throw RemessaException.validacao(
                    RemessaErrorType.DADOS_INVALIDOS,
                    "Período de consulta é obrigatório"
            );
        }

        if (inicio.isAfter(fim)) {
            throw RemessaException.validacao(
                    RemessaErrorType.DADOS_INVALIDOS,
                    "Data inicial não pode ser posterior à data final"
            );
        }

        long diasEntreDatas = ChronoUnit.DAYS.between(inicio.toLocalDate(), fim.toLocalDate());
        if (diasEntreDatas > PERIODO_MAXIMO_DIAS) {
            throw RemessaException.validacao(
                    RemessaErrorType.DADOS_INVALIDOS,
                    "Período de consulta não pode ser superior a " + PERIODO_MAXIMO_DIAS + " dias"
            );
        }

        if (pageable == null) {
            throw RemessaException.validacao(
                    RemessaErrorType.DADOS_INVALIDOS,
                    "Informações de paginação são obrigatórias"
            );
        }

        if (pageable.getPageSize() > TAMANHO_MAXIMO_PAGINA) {
            throw RemessaException.validacao(
                    RemessaErrorType.DADOS_INVALIDOS,
                    "Tamanho máximo da página é " + TAMANHO_MAXIMO_PAGINA
            );
        }
    }

    private void validarCamposObrigatorios(RemessaRequestDTO remessaRequestDTO) {
        Set<ConstraintViolation<RemessaRequestDTO>> violations = validator.validate(remessaRequestDTO);
        if (!violations.isEmpty()) {
            String mensagemErro = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .reduce((m1, m2) -> m1 + "; " + m2)
                    .orElse("Erro de validação");

            log.error("Erro na validação dos campos obrigatórios: {}", mensagemErro);
            throw RemessaException.validacao(
                    RemessaErrorType.DADOS_INVALIDOS,
                    mensagemErro
            );
        }
    }

    private void validarUsuarios(RemessaRequestDTO remessaRequestDTO) {
        if (!usuarioRepository.existsById(remessaRequestDTO.getUsuarioId())) {
            throw RemessaException.negocio(
                    RemessaErrorType.USUARIO_NAO_ENCONTRADO,
                    "Usuário remetente não encontrado"
            );
        }

        if (!usuarioRepository.existsById(remessaRequestDTO.getDestinatarioId())) {
            throw RemessaException.negocio(
                    RemessaErrorType.USUARIO_NAO_ENCONTRADO,
                    "Usuário destinatário não encontrado"
            );
        }
    }

    private void validarRemetenteDestinatario(RemessaRequestDTO remessaRequestDTO) {
        if (remessaRequestDTO.getUsuarioId().equals(remessaRequestDTO.getDestinatarioId())) {
            throw RemessaException.validacao(
                    RemessaErrorType.DADOS_INVALIDOS,
                    "Remetente e destinatário não podem ser o mesmo usuário"
            );
        }
    }
}