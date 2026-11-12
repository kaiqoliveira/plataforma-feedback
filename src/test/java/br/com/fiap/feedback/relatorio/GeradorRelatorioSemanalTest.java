package br.com.fiap.feedback.relatorio;

import br.com.fiap.feedback.dominio.Urgencia;
import br.com.fiap.feedback.suporte.Fakes;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeradorRelatorioSemanalTest {

    private static final Periodo SEMANA = new Periodo(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 15));

    @Test
    void calculaMediaEQuantidadesPorDiaEPorUrgencia() {
        Fakes.RepositorioEmMemoria repo = new Fakes.RepositorioEmMemoria(
                Fakes.avaliacao("a", 10, "2026-11-09T12:00:00Z"),
                Fakes.avaliacao("b", 2, "2026-11-09T15:00:00Z"),
                Fakes.avaliacao("c", 5, "2026-11-11T12:00:00Z"),
                Fakes.avaliacao("d", 8, "2026-11-15T20:00:00Z"),
                Fakes.avaliacao("fora", 0, "2026-11-20T20:00:00Z"));

        RelatorioSemanal r = new GeradorRelatorioSemanal(repo, Fakes.RELOGIO).gerar(SEMANA);

        assertEquals(4, r.totalAvaliacoes());
        assertEquals(6.25, r.mediaNotas());
        assertEquals(7, r.quantidadePorDia().size(), "todos os dias do período, inclusive os zerados");
        assertEquals(2L, r.quantidadePorDia().get(LocalDate.of(2026, 11, 9)));
        assertEquals(0L, r.quantidadePorDia().get(LocalDate.of(2026, 11, 10)));
        assertEquals(1L, r.quantidadePorUrgencia().get(Urgencia.CRITICA));
        assertEquals(1L, r.quantidadePorUrgencia().get(Urgencia.MEDIA));
        assertEquals(2L, r.quantidadePorUrgencia().get(Urgencia.BAIXA));
        assertEquals("a", r.avaliacoes().get(0).descricao());
        assertEquals(1, repo.consultas.size(), "uma única consulta ao banco por relatório");
    }

    @Test
    void semAvaliacoesMediaEhNula() {
        RelatorioSemanal r = new GeradorRelatorioSemanal(new Fakes.RepositorioEmMemoria(), Fakes.RELOGIO).gerar(SEMANA);

        assertEquals(0, r.totalAvaliacoes());
        assertNull(r.mediaNotas());
        assertEquals(0L, r.quantidadePorUrgencia().get(Urgencia.CRITICA));
    }

    @Test
    void periodoDosUltimos7DiasTerminaHojeNoFusoDeSaoPaulo() {
        // RELOGIO = 16/11 02:59 UTC = 15/11 23:59 em São Paulo
        assertEquals(SEMANA, Periodo.ultimos7Dias(Fakes.RELOGIO));
    }

    @Test
    void rejeitaPeriodoInvertido() {
        assertThrows(IllegalArgumentException.class,
                () -> new Periodo(LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 9)));
    }
}
