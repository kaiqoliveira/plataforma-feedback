package br.com.fiap.feedback.relatorio;

import br.com.fiap.feedback.dominio.Urgencia;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Relatório consolidado de um período (por padrão, a semana anterior).
 *
 * @param inicio                primeiro dia do período (inclusive)
 * @param fim                   último dia do período (inclusive)
 * @param geradoEm              instante de geração
 * @param totalAvaliacoes       quantidade total de avaliações
 * @param mediaNotas            média das notas, com 2 casas decimais ({@code null} se não houver avaliações)
 * @param quantidadePorDia      quantidade de avaliações por dia (todos os dias do período, inclusive os zerados)
 * @param quantidadePorUrgencia quantidade de avaliações por urgência (todas as urgências)
 * @param avaliacoes            lista de avaliações (descrição, urgência e data de envio)
 */
public record RelatorioSemanal(
        LocalDate inicio,
        LocalDate fim,
        Instant geradoEm,
        long totalAvaliacoes,
        Double mediaNotas,
        Map<LocalDate, Long> quantidadePorDia,
        Map<Urgencia, Long> quantidadePorUrgencia,
        List<Item> avaliacoes) {

    /** Linha do relatório com os dados de uma avaliação. */
    public record Item(String descricao, Urgencia urgencia, int nota, Instant dataEnvio) {
    }
}
