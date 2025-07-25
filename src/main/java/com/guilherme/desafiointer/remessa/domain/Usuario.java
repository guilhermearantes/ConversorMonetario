
package com.guilherme.desafiointer.remessa.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @PrePersist
    @PreUpdate
    private void validarDocumento() {
        if (tipoUsuario == TipoUsuario.PF && !documento.matches("\\d{11}")) {
            throw new IllegalArgumentException("CPF deve conter 11 dígitos");
        }
        if (tipoUsuario == TipoUsuario.PJ && !documento.matches("\\d{14}")) {
            throw new IllegalArgumentException("CNPJ deve conter 14 dígitos");
        }
    }
}