package com.guilherme.desafiointer.domain;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Entidade que representa um usuario do sistema de remessas.
 *
 * Suporta dois tipos: Pessoa Física (PF) e Pessoa Jurídica (PJ),
 * cada um com limites diários específicos para transferências internacionais.
 *
 * Limites: PF (R$ 10.000/dia) | PJ (R$ 50.000/dia)
 */
@Entity
@Table(name = "usuarios")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nome completo do usuário (pessoa física ou razão social)
     */
    @NotBlank(message = "Nome completo é obrigatório")
    @Column(nullable = false)
    private String nomeCompleto;

    /**
     * Email único do usuário para login e comunicações
     */
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ser válido")
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Documento único: CPF para PF ou CNPJ para PJ
     */
    @NotBlank(message = "Documento é obrigatório")
    @Column(nullable = false, unique = true)
    private String documento;

    /**
     * Senha criptografada para autenticação
     */
    @JsonIgnore
    @NotBlank(message = "Senha é obrigatória")
    @Column(nullable = false)
    private String senha;

    /**
     * Tipo de usuário que determina limites e taxas aplicáveis
     */
    @NotNull(message = "Tipo de usuário é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoUsuario tipoUsuario;

    /**
     * Carteira digital associada com saldos BRL e USD
     */
    @Setter
    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL)
    private Carteira carteira;

    /**
     * Retorna a senha criptografada para validações de autenticação.
     * Metodo protegido por @JsonIgnore para evitar exposição em APIs.
     *
     * @return senha criptografada
     */
    @JsonIgnore
    public String getSenha() {
        return senha;
    }

    /**
     * Define uma nova senha já criptografada.
     * Usado pelo PasswordEncoder para atualizar senhas de forma segura.
     *
     * @param senhaEncoded senha já criptografada
     */
    public void setSenhaEncoded(String senhaEncoded) {
        this.senha = senhaEncoded;
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "id=" + id +
                ", nomeCompleto='" + nomeCompleto + '\'' +
                ", email='" + email + '\'' +
                ", documento='" + documento + '\'' +
                ", tipoUsuario=" + tipoUsuario +
                '}';
    }
}