package br.com.fiap.feedback.dominio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrgenciaTest {

    @Test
    void classificaNotasNosLimitesDasFaixas() {
        assertEquals(Urgencia.CRITICA, Urgencia.daNota(0));
        assertEquals(Urgencia.CRITICA, Urgencia.daNota(3));
        assertEquals(Urgencia.MEDIA, Urgencia.daNota(4));
        assertEquals(Urgencia.MEDIA, Urgencia.daNota(6));
        assertEquals(Urgencia.BAIXA, Urgencia.daNota(7));
        assertEquals(Urgencia.BAIXA, Urgencia.daNota(10));
    }

    @Test
    void rejeitaNotaForaDoIntervalo() {
        assertThrows(AvaliacaoInvalidaException.class, () -> Urgencia.daNota(-1));
        assertThrows(AvaliacaoInvalidaException.class, () -> Urgencia.daNota(11));
    }

    @Test
    void somenteCriticaEhCritica() {
        assertTrue(Urgencia.CRITICA.isCritica());
        assertFalse(Urgencia.MEDIA.isCritica());
        assertFalse(Urgencia.BAIXA.isCritica());
    }
}
