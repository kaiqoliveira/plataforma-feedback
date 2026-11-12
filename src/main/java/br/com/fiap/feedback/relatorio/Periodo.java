package br.com.fiap.feedback.relatorio;

import br.com.fiap.feedback.config.Fuso;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Período coberto pelo relatório.
 *
 * @param inicio primeiro dia (inclusive)
 * @param fim    último dia (inclusive)
 */
public record Periodo(LocalDate inicio, LocalDate fim) {

    public static final int DIAS = 7;

    public Periodo {
        if (fim.isBefore(inicio)) {
            throw new IllegalArgumentException("fim não pode ser anterior ao início");
        }
    }

    /**
     * Os últimos 7 dias, terminando hoje (fuso de São Paulo).
     *
     * <p>O agendamento roda domingo às 23:59 (horário de Brasília), então o relatório cobre de
     * segunda a domingo. Numa execução manual (demonstração), inclui as avaliações do próprio dia.</p>
     */
    public static Periodo ultimos7Dias(Clock relogio) {
        LocalDate hoje = LocalDate.now(relogio.withZone(Fuso.SAO_PAULO));
        return new Periodo(hoje.minusDays(DIAS - 1L), hoje);
    }
}
