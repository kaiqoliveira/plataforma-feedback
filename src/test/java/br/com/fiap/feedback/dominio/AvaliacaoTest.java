package br.com.fiap.feedback.dominio;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AvaliacaoTest {

    private final Clock relogio = Clock.fixed(Instant.parse("2026-11-12T17:42:00Z"), ZoneOffset.UTC);

    @Test
    void criaAvaliacaoComUrgenciaEDataDeEnvio() {
        Avaliacao a = Avaliacao.nova("  Aula muito boa  ", 9, relogio);

        assertNotNull(a.id());
        assertEquals("Aula muito boa", a.descricao());
        assertEquals(9, a.nota());
        assertEquals(Urgencia.BAIXA, a.urgencia());
        assertEquals(Instant.parse("2026-11-12T17:42:00Z"), a.dataEnvio());
    }

    @Test
    void diaDeEnvioUsaFusoDeSaoPaulo() {
        // 01:30 UTC do dia 13 ainda é dia 12 em São Paulo (UTC-3)
        Clock madrugadaUtc = Clock.fixed(Instant.parse("2026-11-13T01:30:00Z"), ZoneOffset.UTC);
        Avaliacao a = Avaliacao.nova("teste", 5, madrugadaUtc);
        assertEquals(LocalDate.of(2026, 11, 12), a.diaEnvio());
    }

    @Test
    void rejeitaDescricaoVaziaOuNula() {
        assertThrows(AvaliacaoInvalidaException.class, () -> Avaliacao.nova(null, 5, relogio));
        assertThrows(AvaliacaoInvalidaException.class, () -> Avaliacao.nova("   ", 5, relogio));
    }

    @Test
    void rejeitaDescricaoMuitoLonga() {
        String longa = "x".repeat(Avaliacao.TAMANHO_MAXIMO_DESCRICAO + 1);
        assertThrows(AvaliacaoInvalidaException.class, () -> Avaliacao.nova(longa, 5, relogio));
    }

    @Test
    void rejeitaNotaAusenteOuInvalida() {
        assertThrows(AvaliacaoInvalidaException.class, () -> Avaliacao.nova("ok", null, relogio));
        assertThrows(AvaliacaoInvalidaException.class, () -> Avaliacao.nova("ok", 15, relogio));
    }
}
