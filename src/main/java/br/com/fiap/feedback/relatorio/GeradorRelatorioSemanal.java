package br.com.fiap.feedback.relatorio;

import br.com.fiap.feedback.dominio.Avaliacao;
import br.com.fiap.feedback.dominio.Urgencia;
import br.com.fiap.feedback.persistencia.AvaliacaoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Consolida as avaliações de um período: média, quantidade por dia e por urgência. */
@ApplicationScoped
public class GeradorRelatorioSemanal {

    private final AvaliacaoRepository repositorio;
    private final Clock relogio;

    public GeradorRelatorioSemanal(AvaliacaoRepository repositorio, Clock relogio) {
        this.repositorio = repositorio;
        this.relogio = relogio;
    }

    public RelatorioSemanal gerar(Periodo periodo) {
        List<Avaliacao> avaliacoes = repositorio.buscarPorPeriodo(periodo.inicio(), periodo.fim());

        // Todos os dias do período aparecem, inclusive os sem avaliações
        Map<LocalDate, Long> porDia = new LinkedHashMap<>();
        for (LocalDate dia = periodo.inicio(); !dia.isAfter(periodo.fim()); dia = dia.plusDays(1)) {
            porDia.put(dia, 0L);
        }
        Map<Urgencia, Long> porUrgencia = new EnumMap<>(Urgencia.class);
        for (Urgencia u : Urgencia.values()) {
            porUrgencia.put(u, 0L);
        }
        for (Avaliacao a : avaliacoes) {
            porDia.computeIfPresent(a.diaEnvio(), (dia, total) -> total + 1);
            porUrgencia.merge(a.urgencia(), 1L, Long::sum);
        }

        List<RelatorioSemanal.Item> itens = avaliacoes.stream()
                .sorted(Comparator.comparing(Avaliacao::dataEnvio))
                .map(a -> new RelatorioSemanal.Item(a.descricao(), a.urgencia(), a.nota(), a.dataEnvio()))
                .toList();

        return new RelatorioSemanal(
                periodo.inicio(), periodo.fim(), relogio.instant(),
                avaliacoes.size(), media(avaliacoes), porDia, porUrgencia, itens);
    }

    private static Double media(List<Avaliacao> avaliacoes) {
        if (avaliacoes.isEmpty()) {
            return null;
        }
        double media = avaliacoes.stream().mapToInt(Avaliacao::nota).average().orElseThrow();
        return BigDecimal.valueOf(media).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
