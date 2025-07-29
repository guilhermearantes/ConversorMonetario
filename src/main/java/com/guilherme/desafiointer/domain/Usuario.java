package com.guilherme.desafiointer.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import java.util.Objects;

/**
 * Entidade que representa um usuário do sistema.
 * Pode ser pessoa física (PF) ou pessoa jurídica (PJ).
 */
@Entity
@Table(name = "usuarios")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
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

    @Setter
    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL)
    private Carteira carteira;

    public void setSenhaEncoded(String senhaEncoded) {
        this.senha = senhaEncoded;
    }

    /**
     * Valida o documento do usuário (CPF ou CNPJ) antes da persistência.
     */
    @PrePersist
    @PreUpdate
    public void validarDocumento() {
        if (documento == null || tipoUsuario == null) {
            throw new IllegalArgumentException("Documento e tipo de usuário são obrigatórios");
        }

        String documentoLimpo = documento.replaceAll("[^0-9]", "");

        if (tipoUsuario == TipoUsuario.PF && documentoLimpo.length() != 11) {
            throw new IllegalArgumentException("CPF deve conter 11 dígitos numéricos");
        }

        if (tipoUsuario == TipoUsuario.PJ && documentoLimpo.length() != 14) {
            throw new IllegalArgumentException("CNPJ deve conter 14 dígitos numéricos");
        }

        if (!tipoUsuario.validarDocumentoEspecifico(documentoLimpo)) {
            String tipoDoc = (tipoUsuario == TipoUsuario.PF) ? "CPF" : "CNPJ";
            log.error("{} inválido: {}", tipoDoc, documento);
            throw new IllegalArgumentException(tipoDoc + " inválido");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Usuario usuario = (Usuario) o;
        if (id == null || usuario.id == null) {
            return false; // Se algum dos IDs for nulo, os objetos não são iguais
        }
        return id.equals(usuario.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : Objects.hash(id);
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