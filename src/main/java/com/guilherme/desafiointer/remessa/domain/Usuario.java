
package com.guilherme.desafiointer.remessa.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Table(name = "usuarios")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome completo é obrigatório")
    @Column(nullable = false)
    private String nomeCompleto;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ser válido")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Column(nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoUsuario tipoUsuario;

    @Column(nullable = false, unique = true)
    private String documento;

    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL)
    private Carteira carteira;

    public void setSenhaEncoded(String senhaEncoded) {
        this.senha = senhaEncoded;
    }

    public void setCarteira(Carteira carteira) {
        this.carteira = carteira;
    }

    @PrePersist
    @PreUpdate
    public void validarDocumento() {
        if (documento == null || tipoUsuario == null) {
            throw new IllegalArgumentException("Documento e tipo de usuário são obrigatórios");
        }

        // Remove caracteres não numéricos (pontos, traços, etc)
        String numerosSemFormatacao = documento.replaceAll("[^0-9]", "");

        if (tipoUsuario == TipoUsuario.PF) {
            if (numerosSemFormatacao.length() != 11) {
                throw new IllegalArgumentException("CPF deve conter 11 dígitos numéricos");
            }
            if (!validarCPF(numerosSemFormatacao)) {
                throw new IllegalArgumentException("CPF inválido");
            }
        }

        if (tipoUsuario == TipoUsuario.PJ) {
            if (numerosSemFormatacao.length() != 14) {
                throw new IllegalArgumentException("CNPJ deve conter 14 dígitos numéricos");
            }
            if (!validarCNPJ(numerosSemFormatacao)) {
                throw new IllegalArgumentException("CNPJ inválido");
            }
        }
    }

    private boolean validarCPF(String cpf) {
        // Verifica se todos os dígitos são iguais
        if (cpf.matches("(\\d)\\1{10}")) return false;

        // Calcula primeiro dígito verificador
        int soma = 0;
        for (int i = 0; i < 9; i++) {
            soma += Character.getNumericValue(cpf.charAt(i)) * (10 - i);
        }
        int primeiroDigito = 11 - (soma % 11);
        if (primeiroDigito > 9) primeiroDigito = 0;

        // Calcula segundo dígito verificador
        soma = 0;
        for (int i = 0; i < 10; i++) {
            soma += Character.getNumericValue(cpf.charAt(i)) * (11 - i);
        }
        int segundoDigito = 11 - (soma % 11);
        if (segundoDigito > 9) segundoDigito = 0;

        // Verifica se os dígitos calculados são iguais aos dígitos informados
        return cpf.charAt(9) - '0' == primeiroDigito &&
                cpf.charAt(10) - '0' == segundoDigito;
    }

    private boolean validarCNPJ(String cnpj) {
        // Verifica se todos os dígitos são iguais
        if (cnpj.matches("(\\d)\\1{13}")) return false;

        // Calcula primeiro dígito verificador
        int[] multiplicadoresPrimeiroDigito = {5,4,3,2,9,8,7,6,5,4,3,2};
        int soma = 0;
        for (int i = 0; i < 12; i++) {
            soma += Character.getNumericValue(cnpj.charAt(i)) * multiplicadoresPrimeiroDigito[i];
        }
        int primeiroDigito = 11 - (soma % 11);
        if (primeiroDigito >= 10) primeiroDigito = 0;

        // Calcula segundo dígito verificador
        int[] multiplicadoresSegundoDigito = {6,5,4,3,2,9,8,7,6,5,4,3,2};
        soma = 0;
        for (int i = 0; i < 13; i++) {
            soma += Character.getNumericValue(cnpj.charAt(i)) * multiplicadoresSegundoDigito[i];
        }
        int segundoDigito = 11 - (soma % 11);
        if (segundoDigito >= 10) segundoDigito = 0;

        // Verifica se os dígitos calculados são iguais aos dígitos informados
        return cnpj.charAt(12) - '0' == primeiroDigito &&
                cnpj.charAt(13) - '0' == segundoDigito;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Usuario usuario = (Usuario) o;
        return Objects.equals(id, usuario.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "id=" + id +
                ", nomeCompleto='" + nomeCompleto + '\'' +
                ", email='" + email + '\'' +
                ", tipoUsuario=" + tipoUsuario +
                ", documento='" + documento + '\'' +
                '}';
    }
}