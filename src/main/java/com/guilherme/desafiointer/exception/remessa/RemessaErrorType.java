package com.guilherme.desafiointer.exception.remessa;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum RemessaErrorType {
    // Erros de Validação
    DADOS_INVALIDOS("Dados inválidos para a operação", HttpStatus.BAD_REQUEST),
    PERIODO_INVALIDO("Período de consulta inválido", HttpStatus.BAD_REQUEST),
    PAGINACAO_INVALIDA("Parâmetros de paginação inválidos", HttpStatus.BAD_REQUEST),
    MOEDA_NAO_SUPORTADA("Moeda não suportada", HttpStatus.BAD_REQUEST),
    VALOR_REMESSA_INVALIDO("Valor da remessa inválido", HttpStatus.BAD_REQUEST),
    USUARIOS_INVALIDOS("Configuração inválida de usuários", HttpStatus.BAD_REQUEST),
    SENHA_INVALIDA("Credenciais inválidas", HttpStatus.UNAUTHORIZED),

    // Erros de Usuário
    USUARIO_NAO_ENCONTRADO("Usuário não encontrado", HttpStatus.NOT_FOUND),
    DOCUMENTO_JA_CADASTRADO("Documento já cadastrado", HttpStatus.CONFLICT),
    EMAIL_JA_CADASTRADO("Email já cadastrado", HttpStatus.CONFLICT),

    // Erros de Carteira e Limites
    SALDO_INSUFICIENTE("Saldo insuficiente para realizar a operação", HttpStatus.UNPROCESSABLE_ENTITY),
    LIMITE_DIARIO_EXCEDIDO("Limite diário de transações excedido", HttpStatus.UNPROCESSABLE_ENTITY),
    CARTEIRA_NAO_ENCONTRADA("Carteira não encontrada", HttpStatus.NOT_FOUND),

    // Erros de Concorrência
    OPERACAO_EM_ANDAMENTO("Operação em andamento para este usuário", HttpStatus.CONFLICT),

    // Erros de Integração Externa
    ERRO_COTACAO("Erro ao obter cotação da moeda", HttpStatus.SERVICE_UNAVAILABLE),

    // Erros de Processamento
    ERRO_PROCESSAMENTO("Erro no processamento da operação", HttpStatus.INTERNAL_SERVER_ERROR),
    ERRO_PROCESSAMENTO_USUARIO("Erro no processamento do usuário", HttpStatus.INTERNAL_SERVER_ERROR),
    ERRO_PERSISTENCIA("Erro ao persistir dados", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String message;
    private final HttpStatus httpStatus;

    RemessaErrorType(String message, HttpStatus httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }
}