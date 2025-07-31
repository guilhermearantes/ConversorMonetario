package com.guilherme.desafiointer.domain;

import com.guilherme.desafiointer.service.validator.DocumentoValidationHolder;
import lombok.Getter;

/**
 * Enum que representa os tipos de usuario no sistema de remessas internacionais.
 * Define as características específicas, validações de documento e regras de negócio
 * para cada categoria de usuario no contexto de transferências internacionais.
 */
@Getter
public enum TipoUsuario {

    /**
     * Pessoa Física - Usuario individual do sistema.
     *
     * Características:
     * - Documento: CPF com 11 dígitos numéricos
     * - Limite diário: R$ 10.000,00 para remessas
     * - Taxa aplicada: 2% sobre o valor da remessa
     * - Validação: Algoritmo oficial de CPF da Receita Federal
     *
     * Uso típico:
     * - Pessoas físicas realizando remessas pessoais
     * - Transferências para familiares no exterior
     * - Pagamentos pessoais internacionais
     */
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

    /**
     * Pessoa Jurídica - Usuario empresarial do sistema.
     * Características:
     * - Documento: CNPJ com 14 dígitos numéricos
     * - Limite diário: R$ 50.000,00 para remessas
     * - Taxa aplicada: 1% sobre o valor da remessa (taxa corporativa)
     * - Validação: Algoritmo oficial de CNPJ da Receita Federal
     * Uso típico:
     * - Empresas realizando pagamentos internacionais
     * - Transferências corporativas para filiais
     * - Pagamentos a fornecedores no exterior
     * - Remessas comerciais de grande volume
     * Vantagens corporativas:
     * - Limite diário 5x maior que PF
     * - Taxa reduzida (50% menor que PF)
     * - Adequado para operações comerciais frequentes
     */
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
     * Valida o documento conforme o tipo de usuario.
     * Remove caracteres não numéricos antes da validação.
     * Processo de validação:
     * 1. Verifica se o documento não é nulo
     * 2. Remove todos os caracteres não numéricos (pontos, hífens, barras)
     * 3. Verifica se o tamanho corresponde ao esperado para o tipo
     * 4. Executa validação específica do algoritmo (CPF ou CNPJ)
     *
     * @param documento documento a ser validado (pode conter formatação)
     * @return true se o documento é válido segundo as regras da Receita Federal
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
     * Implementado por cada constante do enum para aplicar:
     * - Algoritmo de validação de CPF para PF
     * - Algoritmo de validação de CNPJ para PJ
     *
     * @param documento documento a ser validado (apenas números, sem formatação)
     * @return true se o documento é válido segundo o algoritmo específico
     */
    protected abstract boolean validarDocumentoEspecifico(String documento);

    /**
     * Retorna a mensagem de formato esperado para o documento.
     * Usado para ‘feedback’ ao usuario em caso de formato inválido.
     * Cada tipo retorna a sua mensagem específica de orientação.
     *
     * @return mensagem descritiva do formato esperado para o tipo
     */
    public abstract String getFormatoDocumento();

    /**
     * Retorna uma representação textual legível do tipo de usuario.
     *
     * @return descrição amigável do tipo (ex: "Pessoa Física", "Pessoa Jurídica")
     */
    @Override
    public String toString() {
        return this.descricao;
    }
}