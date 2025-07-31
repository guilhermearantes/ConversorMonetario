package com.guilherme.desafiointer.repository;

import com.guilherme.desafiointer.domain.CotacaoHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

/**
 * Repository para operações de consulta ao histórico de cotações.
 *
 * Fornece métodos especializados para buscar cotações históricas
 * com diferentes estratégias de fallback, garantindo continuidade
 * do serviço mesmo quando a API do Banco Central está indisponível.
 *
 * Funcionalidades principais:
 * - Busca da última cotação disponível (qualquer dia)
 * - Busca da última cotação útil (somente dias úteis)
 * - Suporte a fallback em fins de semana/feriados
 * - Queries otimizadas para performance
 */
public interface CotacaoHistoricoRepository extends JpaRepository<CotacaoHistorico, Long> {

    /**
     * Busca a última cotação disponível para uma moeda específica.
     * Retorna a cotação mais recente independentemente se foi
     * obtida em dia útil ou fim de semana. Útil quando qualquer
     * cotação histórica serve como fallback.
     * Query otimizada com subquery para encontrar a data/hora
     * mais recente e depois buscar o registro correspondente.
     * @param moeda código da moeda (ex: "USD", "BRL")
     * @return Optional contendo a última cotação ou empty se não existir
     * Casos de uso:
     * - Fallback geral quando API BCB está indisponível
     * - Recuperação de cache após restart da aplicação
     * - Validação de dados históricos em testes
     */
    @Query("SELECT c FROM CotacaoHistorico c WHERE c.moeda = :moeda " +
            "AND c.dataHora = (SELECT MAX(ch.dataHora) FROM CotacaoHistorico ch WHERE ch.moeda = :moeda)")
    Optional<CotacaoHistorico> findUltimaCotacao(@Param("moeda") String moeda);

    /**
     * Busca a última cotação útil (dia útil) para uma moeda específica.
     * Filtra cotações obtidas apenas em dias úteis (isFimDeSemana = false)
     * e retorna a mais recente. Estratégia preferencial para fallback,
     * pois garante cotação "oficial" do Banco Central.
     * Query ordenada por dataHora DESC com LIMIT implícito via Optional.
     * Performance otimizada para grandes volumes de dados históricos.
     *
     * @param moeda código da moeda (ex: "USD", "EUR")
     * @return Optional contendo a última cotação útil ou empty se não existir
     * Casos de uso:
     * - Fallback em fins de semana (usa cotação da sexta-feira)
     * - Feriados prolongados (última cotação oficial)
     * - Garantia de precisão em cálculos financeiros críticos
     * - Compliance com regulamentações do BCB
     */
    @Query("SELECT c FROM CotacaoHistorico c " +
            "WHERE c.moeda = :moeda " +
            "AND c.isFimDeSemana = false " +
            "ORDER BY c.dataHora DESC")
    Optional<CotacaoHistorico> findUltimaCotacaoUtil(@Param("moeda") String moeda);
}