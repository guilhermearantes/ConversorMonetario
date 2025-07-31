package com.guilherme.desafiointer.service.processor;

import com.guilherme.desafiointer.domain.Remessa;
import com.guilherme.desafiointer.domain.Usuario;
import com.guilherme.desafiointer.dto.remessa.RemessaRequestDTO;
import com.guilherme.desafiointer.exception.domain.SaldoInsuficienteException;
import com.guilherme.desafiointer.exception.remessa.RemessaException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

public interface RemessaProcessor {

    /**
     * Processa remessa internacional completa com validações e conversões.
     *
     * Executa sequencialmente:
     * 1. Busca carteiras com lock pessimista
     * 2. Obtém cotação oficial com cache
     * 3. Calcula taxa baseada no tipo de usuário
     * 4. Converte valores entre moedas (BRL ↔ USD)
     * 5. Valida saldo e limites diários
     * 6. Executa débito/crédito transacional
     * 7. Persiste remessa e atualiza histórico
     * 8. Limpa caches para consistência
     *
     * @param remessaRequestDTO dados da transferência incluindo usuários, valor e moeda
     * @return Remessa persistida com valores calculados e timestamp
     * @throws RemessaException quando regras de negócio são violadas
     * @throws SaldoInsuficienteException quando saldo inadequado
     * @throws IllegalArgumentException quando moeda não suportada
     */
    Remessa processarRemessa(RemessaRequestDTO remessaRequestDTO);

    /**
     * Busca histórico paginado de remessas com cache automático.
     *
     * Utiliza cache inteligente baseado em:
     * - ID do usuário consultante
     * - Período de consulta (início/fim)
     * - Número da página solicitada
     *
     * Cache evitado quando:
     * - Consultas muito recentes (dados podem estar mudando)
     * - Períodos muito amplos (baixa probabilidade de reutilização)
     *
     * @param usuario usuário proprietário das remessas
     * @param inicio data/hora inicial do período (inclusive)
     * @param fim data/hora final do período (inclusive)
     * @param pageable configuração de paginação (página, tamanho, ordenação)
     * @return página de remessas ordenadas por data decrescente
     * @throws IllegalArgumentException quando período inválido
     */
    Page<Remessa> buscarHistorico(Usuario usuario, LocalDateTime inicio,
                                  LocalDateTime fim, Pageable pageable);
}