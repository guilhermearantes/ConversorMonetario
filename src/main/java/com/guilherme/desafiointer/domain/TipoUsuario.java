
package com.guilherme.desafiointer.domain;

import com.guilherme.desafiointer.service.validator.DocumentoValidationHolder;
import lombok.Getter;

/**
 * Enum que representa os tipos de usuário do sistema.
 * Define as características e validações específicas para cada tipo.
 */
@Getter
public enum TipoUsuario {

    PF("Pessoa Física", 11) {
        @Override
        public boolean validarDocumentoEspecifico(String documento) {
            return DocumentoValidationHolder.getService().validarCPF(documento);
        }

        @Override
        public String getFormatoDocumento() {
            return "CPF deve conter 11 dígitos numéricos";
        }
    },

    PJ("Pessoa Jurídica", 14) {
        @Override
        public boolean validarDocumentoEspecifico(String documento) {
            return DocumentoValidationHolder.getService().validarCNPJ(documento);
        }

        @Override
        public String getFormatoDocumento() {
            return "CNPJ deve conter 14 dígitos numéricos";
        }
    };

    private final String descricao;
    private final int tamanhoDocumento;

    /**
     * Construtor do enum TipoUsuario.
     *
     * @param descricao descrição do tipo de usuário
     * @param tamanhoDocumento tamanho esperado do documento (11 para CPF, 14 para CNPJ)
     */
    TipoUsuario(String descricao, int tamanhoDocumento) {
        this.descricao = descricao;
        this.tamanhoDocumento = tamanhoDocumento;
    }

    /**
     * Valida o documento de acordo com o tipo de usuário.
     * Remove caracteres não numéricos antes da validação.
     *
     * @param documento documento a ser validado
     * @return true se o documento é válido, false caso contrário
     */
    public boolean validarDocumento(String documento) {
        if (documento == null) {
            return false;
        }
        String apenasNumeros = documento.replaceAll("[^0-9]", "");
        return apenasNumeros.length() == this.tamanhoDocumento &&
                validarDocumentoEspecifico(apenasNumeros);
    }

    /**
     * Metodo abstrato para validação específica do documento de acordo com o tipo.
     *
     * @param documento documento a ser validado (apenas números)
     * @return true se o documento é válido, false caso contrário
     */
    protected abstract boolean validarDocumentoEspecifico(String documento);

    /**
     * Retorna a mensagem de formato esperado para o documento.
     *
     * @return mensagem descritiva do formato esperado
     */
    public abstract String getFormatoDocumento();

    @Override
    public String toString() {
        return this.descricao;
    }
}