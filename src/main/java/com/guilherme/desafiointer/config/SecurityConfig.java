package com.guilherme.desafiointer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuração de segurança da aplicação.
 *
 * Define os beans necessários para criptografia e validação de senhas
 * dos usuários do sistema. Utiliza BCrypt como algoritmo padrão por
 * ser seguro e amplamente aceito pela comunidade.
 *
 * BCrypt características:
 * - Algoritmo adaptativo que aumenta o custo computacional ao longo do tempo
 * - Salt automático para evitar ataques de rainbow table
 * - Resistente a ataques de força bruta
 *
 * Usado pelo UsuarioService para:
 * - Criptografar senhas no cadastro
 * - Validar senhas no login (futuro)
 */
@Configuration
public class SecurityConfig {

    /**
     * Cria o encoder de senhas usando BCrypt.
     *
     * BCrypt é um algoritmo de hash adaptativo baseado no cipher Blowfish.
     * É especificamente projetado para senhas e inclui salt automático.
     *
     * @return PasswordEncoder configurado com BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}