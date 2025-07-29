package com.guilherme.desafiointer.repository;

import com.guilherme.desafiointer.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório responsável pelas operações de persistência de usuários.
 * Fornece métodos de busca e validação de unicidade de documentos e emails.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca um usuário pelo seu email.
     *
     * @param email endereço de email do usuário
     * @return Optional<Usuario> contendo o usuário se encontrado
     */
    Optional<Usuario> findByEmail(String email);

    /**
     * Busca um usuário pelo seu documento (CPF/CNPJ).
     *
     * @param documento número do documento do usuário
     * @return Optional<Usuario> contendo o usuário se encontrado
     */
    Optional<Usuario> findByDocumento(String documento);

    /**
     * Verifica se existe um usuário com o email informado.
     *
     * @param email endereço de email para verificação
     * @return boolean indicando se o email já está cadastrado
     */
    boolean existsByEmail(String email);

    /**
     * Verifica se existe um usuário com o documento informado.
     *
     * @param documento número do documento para verificação
     * @return boolean indicando se o documento já está cadastrado
     */
    boolean existsByDocumento(String documento);
}