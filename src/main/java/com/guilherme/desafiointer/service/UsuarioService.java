package com.guilherme.desafiointer.service;

import com.guilherme.desafiointer.domain.Carteira;
import com.guilherme.desafiointer.domain.TipoUsuario;
import com.guilherme.desafiointer.domain.Usuario;
import com.guilherme.desafiointer.exception.remessa.RemessaErrorType;
import com.guilherme.desafiointer.exception.remessa.RemessaException;
import com.guilherme.desafiointer.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

/**
 * Serviço responsável pelo gerenciamento de usuários do sistema.
 * Implementa operações internas e regras de negócio específicas para usuários.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Cria usuário completo com carteira inicializada.
     * Valida unicidade, criptografa senha e configura carteira zerada.
     */
    @Transactional
    public Usuario criarUsuario(String nomeCompleto, String email, String senha,
                                TipoUsuario tipoUsuario, String documento) {
        // Valida a unicidade do email e do documento
        validarUsuarioUnico(email, documento);

        try {
            // Criação do usuário
            Usuario usuario = Usuario.builder()
                    .nomeCompleto(nomeCompleto)
                    .email(email)
                    .senha(passwordEncoder.encode(senha))
                    .tipoUsuario(tipoUsuario)
                    .documento(documento)
                    .build();

            // Inicializa a carteira com saldos distintos para BRL e USD
            Carteira carteira = Carteira.builder()
                    .usuario(usuario)
                    .saldoBRL(BigDecimal.ZERO)
                    .saldoUSD(BigDecimal.ZERO)
                    .build();

            // Associa a carteira ao usuário
            usuario.setCarteira(carteira);

            // Salva o usuário e a carteira
            return usuarioRepository.save(usuario);
        } catch (Exception e) {
            log.error("Erro ao criar usuário: {}", e.getMessage());
            throw RemessaException.processamento(
                    RemessaErrorType.ERRO_PROCESSAMENTO_USUARIO,
                    "Erro ao criar usuário",
                    e
            );
        }
    }

    private void validarUsuarioUnico(String email, String documento) {
        if (usuarioRepository.existsByDocumento(documento)) {
            throw RemessaException.validacao(
                    RemessaErrorType.DOCUMENTO_JA_CADASTRADO,
                    "Documento já cadastrado: " + documento
            );
        }

        if (usuarioRepository.existsByEmail(email)) {
            throw RemessaException.validacao(
                    RemessaErrorType.EMAIL_JA_CADASTRADO,
                    "Email já cadastrado: " + email
            );
        }
    }

    /**
     * Altera senha com validação de senha atual.
     * Verifica autenticidade antes de permitir alteração.
     */
    @Transactional
    public void alterarSenha(Long userId, String senhaAtual, String novaSenha) {
        Usuario usuario = buscarPorId(userId);

        if (!passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
            throw RemessaException.validacao(
                    RemessaErrorType.SENHA_INVALIDA,
                    "Senha atual incorreta"
            );
        }

        try {
            usuario.setSenhaEncoded(passwordEncoder.encode(novaSenha));
            usuarioRepository.save(usuario);
        } catch (Exception e) {
            log.error("Erro ao alterar senha: {}", e.getMessage());
            throw RemessaException.processamento(
                    RemessaErrorType.ERRO_PROCESSAMENTO_USUARIO,
                    "Erro ao alterar senha",
                    e
            );
        }
    }

    /**
     * Busca usuario por ID com exception se não encontrado.
     */
    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> RemessaException.negocio(
                        RemessaErrorType.USUARIO_NAO_ENCONTRADO,
                        "Usuário não encontrado com ID: " + id
                ));
    }

    /**
     * Busca usuario por documento com exception se não encontrado.
     */
    public Usuario buscarPorDocumento(String documento) {
        return usuarioRepository.findByDocumento(documento)
                .orElseThrow(() -> RemessaException.negocio(
                        RemessaErrorType.USUARIO_NAO_ENCONTRADO,
                        "Usuário não encontrado com documento: " + documento
                ));
    }

    /**
     * Valida unicidade condicional em atualizações de usuário.
     * Só verifica conflitos se email/documento realmente mudaram.
     *
     * @param usuario usuário atual
     * @param novoEmail novo email (pode ser igual ao atual)
     * @param novoDocumento novo documento (pode ser igual ao atual)
     * @throws RemessaException se email/documento já existe para outro usuário
     */
    private void validarAtualizacaoUsuario(Usuario usuario, String novoEmail, String novoDocumento) {
        if (!usuario.getEmail().equals(novoEmail) &&
                usuarioRepository.existsByEmail(novoEmail)) {
            throw RemessaException.validacao(
                    RemessaErrorType.EMAIL_JA_CADASTRADO,
                    "Email já cadastrado para outro usuário: " + novoEmail
            );
        }

        if (!usuario.getDocumento().equals(novoDocumento) &&
                usuarioRepository.existsByDocumento(novoDocumento)) {
            throw RemessaException.validacao(
                    RemessaErrorType.DOCUMENTO_JA_CADASTRADO,
                    "Documento já cadastrado para outro usuário: " + novoDocumento
            );
        }
    }

    /**
     * FUNCIONALIDADE FUTURA
     * Busca usuario por email com exception se não encontrado.
     */
    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> RemessaException.negocio(
                        RemessaErrorType.USUARIO_NAO_ENCONTRADO,
                        "Usuário não encontrado com email: " + email
                ));
    }

    /**
     * FUNCIONALIDADE FUTURA
     * Lista todos os usuários com tratamento de erro.
     */
    public List<Usuario> listarTodos() {
        try {
            return usuarioRepository.findAll();
        } catch (Exception e) {
            log.error("Erro ao listar usuários: {}", e.getMessage());
            throw RemessaException.processamento(
                    RemessaErrorType.ERRO_PROCESSAMENTO_USUARIO,
                    "Erro ao listar usuários",
                    e
            );
        }
    }

    /**
     * FUNCIONALIDADE FUTURA
     * Atualiza dados do usuário preservando senha e carteira.
     * Valida unicidade apenas para campos alterados.
     */
    @Transactional
    public Usuario atualizarUsuario(Long id, String nomeCompleto, String email,
                                    TipoUsuario tipoUsuario, String documento) {
        Usuario usuarioExistente = buscarPorId(id);
        validarAtualizacaoUsuario(usuarioExistente, email, documento);

        try {
            Usuario usuarioAtualizado = Usuario.builder()
                    .id(usuarioExistente.getId())
                    .nomeCompleto(nomeCompleto)
                    .email(email)
                    .senha(usuarioExistente.getSenha())
                    .tipoUsuario(tipoUsuario)
                    .documento(documento)
                    .carteira(usuarioExistente.getCarteira())
                    .build();

            return usuarioRepository.save(usuarioAtualizado);
        } catch (Exception e) {
            log.error("Erro ao atualizar usuário: {}", e.getMessage());
            throw RemessaException.processamento(
                    RemessaErrorType.ERRO_PROCESSAMENTO_USUARIO,
                    "Erro ao atualizar usuário",
                    e
            );
        }
    }
}