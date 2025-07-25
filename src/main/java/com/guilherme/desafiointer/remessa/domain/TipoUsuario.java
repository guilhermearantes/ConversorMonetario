
package com.guilherme.desafiointer.remessa.domain;

import java.math.BigDecimal;

/**
 * Enumeration que representa os tipos de usuário suportados no sistema.
 * Cada tipo possui suas próprias regras e limites de transação.
 */
public enum TipoUsuario {
    /**
     * Pessoa Física - limite diário de R$ 10.000,00
     */
    PF("Pessoa Física", new BigDecimal("10000.00"), 11),

    /**
     * Pessoa Jurídica - limite diário de R$ 50.000,00
     */
    PJ("Pessoa Jurídica", new BigDecimal("50000.00"), 14);

    private final String descricao;
    private final BigDecimal limiteDiario;
    private final int tamanhoDocumento;

    /**
     * Construtor privado para o enum.
     *
     * @param descricao        Descrição amigável do tipo de usuário
     * @param limiteDiario     Limite diário para transações
     * @param tamanhoDocumento Tamanho esperado do documento (CPF ou CNPJ)
     */
    TipoUsuario(String descricao, BigDecimal limiteDiario, int tamanhoDocumento) {
        this.descricao = descricao;
        this.limiteDiario = limiteDiario;
        this.tamanhoDocumento = tamanhoDocumento;
    }

    /**
     * Retorna a descrição amigável do tipo de usuário.
     *
     * @return String contendo a descrição
     */
    public String getDescricao() {
        return descricao;
    }

    /**
     * Retorna o limite diário de transações para o tipo de usuário.
     *
     * @return BigDecimal representando o valor limite
     */
    public BigDecimal getLimiteDiario() {
        return limiteDiario;
    }

    /**
     * Retorna o tamanho esperado do documento para este tipo de usuário.
     *
     * @return int representando o número de dígitos esperado
     */
    public int getTamanhoDocumento() {
        return tamanhoDocumento;
    }

    /**
     * Verifica se o valor fornecido excede o limite diário do tipo de usuário.
     *
     * @param valor Valor a ser verificado
     * @return true se o valor excede o limite, false caso contrário
     */
    public boolean excedeLimite(BigDecimal valor) {
        return valor.compareTo(this.limiteDiario) > 0;
    }

    /**
     * Verifica se o documento fornecido tem o tamanho correto para o tipo de usuário.
     *
     * @param documento Documento a ser validado (apenas números)
     * @return true se o tamanho está correto, false caso contrário
     */
    public boolean validaTamanhoDocumento(String documento) {
        if (documento == null) {
            return false;
        }
        String apenasNumeros = documento.replaceAll("[^0-9]", "");
        return apenasNumeros.length() == this.tamanhoDocumento;
    }

    /**
     * Retorna uma representação textual amigável do tipo de usuário.
     *
     * @return String contendo a descrição do tipo de usuário
     */
    @Override
    public String toString() {
        return this.descricao;
    }
}