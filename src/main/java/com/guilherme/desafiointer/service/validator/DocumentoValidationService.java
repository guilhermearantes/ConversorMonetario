
package com.guilherme.desafiointer.service.validator;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DocumentoValidationService {

    public boolean validarCPF(String cpf) {
        if (cpf == null) return false;

        // Remove caracteres especiais
        String cpfLimpo = cpf.replaceAll("[^0-9]", "");

        // Verifica se tem 11 dígitos
        if (cpfLimpo.length() != 11) {
            return false;
        }

        // Verifica se todos os dígitos são iguais
        if (cpfLimpo.matches("(\\d)\\1{10}")) {
            return false;
        }

        try {
            int[] numeros = new int[11];
            for (int i = 0; i < 11; i++) {
                numeros[i] = Character.getNumericValue(cpfLimpo.charAt(i));
            }

            // Primeiro dígito verificador
            int soma = 0;
            int peso = 10;
            for (int i = 0; i < 9; i++) {
                soma += numeros[i] * peso;
                peso--;
            }

            int digitoVerificador1 = 11 - (soma % 11);
            if (digitoVerificador1 >= 10) {
                digitoVerificador1 = 0;
            }

            if (numeros[9] != digitoVerificador1) {
                return false;
            }

            // Segundo dígito verificador
            soma = 0;
            peso = 11;
            for (int i = 0; i < 10; i++) {
                soma += numeros[i] * peso;
                peso--;
            }

            int digitoVerificador2 = 11 - (soma % 11);
            if (digitoVerificador2 >= 10) {
                digitoVerificador2 = 0;
            }

            return numeros[10] == digitoVerificador2;

        } catch (Exception e) {
            log.error("Erro ao validar CPF: {}", e.getMessage());
            return false;
        }
    }

    public boolean validarCNPJ(String cnpj) {
        if (cnpj == null) return false;

        // Remove caracteres especiais
        String cnpjLimpo = cnpj.replaceAll("[^0-9]", "");

        // Verifica se tem 14 dígitos
        if (cnpjLimpo.length() != 14) {
            return false;
        }

        // Verifica se todos os dígitos são iguais
        if (cnpjLimpo.matches("(\\d)\\1{13}")) {
            return false;
        }

        try {
            int[] numeros = new int[14];
            for (int i = 0; i < 14; i++) {
                numeros[i] = Character.getNumericValue(cnpjLimpo.charAt(i));
            }

            // Primeiro dígito verificador
            int soma = 0;
            int[] pesos1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

            for (int i = 0; i < 12; i++) {
                soma += numeros[i] * pesos1[i];
            }

            int digitoVerificador1 = soma % 11;
            digitoVerificador1 = (digitoVerificador1 < 2) ? 0 : 11 - digitoVerificador1;

            if (numeros[12] != digitoVerificador1) {
                return false;
            }

            // Segundo dígito verificador
            soma = 0;
            int[] pesos2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

            for (int i = 0; i < 13; i++) {
                soma += numeros[i] * pesos2[i];
            }

            int digitoVerificador2 = soma % 11;
            digitoVerificador2 = (digitoVerificador2 < 2) ? 0 : 11 - digitoVerificador2;

            return numeros[13] == digitoVerificador2;

        } catch (Exception e) {
            log.error("Erro ao validar CNPJ: {}", e.getMessage());
            return false;
        }
    }
}