package br.com.fiap.feedback.servico;

import br.com.fiap.feedback.suporte.Fakes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificarUrgenciaServiceTest {

    private final Fakes.NotificadorEmMemoria notificador = new Fakes.NotificadorEmMemoria();
    private final NotificarUrgenciaService servico = new NotificarUrgenciaService(notificador);

    @Test
    void enviaEmailComDescricaoUrgenciaEDataDeEnvio() {
        boolean enviada = servico.notificar(
                Fakes.avaliacao("Não consegui acessar o vídeo", 2, "2026-11-10T13:00:00Z"));

        assertTrue(enviada);
        assertEquals(1, notificador.mensagens.size());
        String corpo = notificador.mensagens.get(0);
        assertTrue(notificador.assuntos.get(0).startsWith("[URGENTE]"));
        assertTrue(corpo.contains("Descrição: Não consegui acessar o vídeo"));
        assertTrue(corpo.contains("Urgência: CRITICA"));
        // 13:00 UTC = 10:00 em Brasília
        assertTrue(corpo.contains("Data de envio: 10/11/2026 10:00"));
    }

    @Test
    void naoNotificaAvaliacaoNaoCritica() {
        assertFalse(servico.notificar(Fakes.avaliacao("Boa aula", 9, "2026-11-10T13:00:00Z")));
        assertTrue(notificador.mensagens.isEmpty());
    }
}
