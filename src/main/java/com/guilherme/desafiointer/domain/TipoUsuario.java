package com.guilherme.desafiointer.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Enumeration que representa os tipos de usuário suportados no sistema.
 * Cada tipo possui suas próprias regras de negócio, limites e taxas.
 */
public enum TipoUsuario {
    /**
     * Pessoa Física - limite diário de R$ 10.000,00 e taxa de 2%
     */
    PF("Pessoa Física",
            new BigDecimal("10000.00"),
            11,
            new BigDecimal("0.02")) {
        @Override
        public boolean validarDocumentoEspecifico(String documento) {
            return validarCPF(documento);
        }

        @Override
        public String getFormatoDocumento() {
            return "CPF deve conter 11 dígitos numéricos";
        }
    },

    /**
     * Pessoa Jurídica - limite diário de R$ 50.000,00 e taxa de 1%
     */
    PJ("Pessoa Jurídica",
            new BigDecimal("50000.00"),
            14,
            new BigDecimal("0.01")) {
        @Override
        public boolean validarDocumentoEspecifico(String documento) {
            return validarCNPJ(documento);
        }

        @Override
        public String getFormatoDocumento() {
            return "CNPJ deve conter 14 dígitos numéricos";
        }
    };

    private final String descricao;
    private final BigDecimal limiteDiario;
    private final int tamanhoDocumento;
    private final BigDecimal taxaPercentual;

    /**
     * Construtor para o enum TipoUsuario.
     */
    TipoUsuario(String descricao,
                BigDecimal limiteDiario,
                int tamanhoDocumento,
                BigDecimal taxaPercentual) {
        this.descricao = descricao;
        this.limiteDiario = limiteDiario;
        this.tamanhoDocumento = tamanhoDocumento;
        this.taxaPercentual = taxaPercentual;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getLimiteDiario() {
        return limiteDiario;
    }

    public int getTamanhoDocumento() {
        return tamanhoDocumento;
    }

    /**
     * Calcula a taxa para uma transação baseada no valor.
     * @param valor Valor da transação
     * @return Taxa calculada
     */
    public BigDecimal calcularTaxa(BigDecimal valor) {
        return valor.multiply(taxaPercentual)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Verifica se o valor fornecido excede o limite diário.
     */
    public boolean excedeLimite(BigDecimal valor) {
        return valor.compareTo(this.limiteDiario) > 0;
    }

    /**
     * Valida o formato e o tamanho do documento.
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
     * Método abstrato para validação específica de cada tipo de documento.
     */
    protected abstract boolean validarDocumentoEspecifico(String documento);

    /**
     * Retorna a mensagem de formato esperado para o documento.
     */
    public abstract String getFormatoDocumento();

    /**
     * Validação de CPF.
     */
    protected static boolean validarCPF(String cpf) {
        if (cpf.matches("(\\d)\\1{10}")) return false;

        int[] multiplicadores1 = {10, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] multiplicadores2 = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2};

        String cpfSemDigitos = cpf.substring(0, 9);
        String digito1 = calcularDigitoVerificador(cpfSemDigitos, multiplicadores1);
        String digito2 = calcularDigitoVerificador(cpfSemDigitos + digito1, multiplicadores2);

        return cpf.equals(cpfSemDigitos + digito1 + digito2);
    }

    /**
     * Validação de CNPJ.
     */
    protected static boolean validarCNPJ(String cnpj) {
        if (cnpj.matches("(\\d)\\1{13}")) return false;

        int[] multiplicadores1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] multiplicadores2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

        String cnpjSemDigitos = cnpj.substring(0, 12);
        String digito1 = calcularDigitoVerificador(cnpjSemDigitos, multiplicadores1);
        String digito2 = calcularDigitoVerificador(cnpjSemDigitos + digito1, multiplicadores2);

        return cnpj.equals(cnpjSemDigitos + digito1 + digito2);
    }

    /**
     * Calcula dígito verificador para documentos.
     */
    private static String calcularDigitoVerificador(String str, int[] multiplicadores) {
        int soma = 0;
        for (int i = 0; i < multiplicadores.length; i++) {
            soma += Character.getNumericValue(str.charAt(i)) * multiplicadores[i];
        }
        int resto = 11 - (soma % 11);
        return String.valueOf(resto > 9 ? 0 : resto);
    }

    @Override
    public String toString() {
        return this.descricao;
    }
}