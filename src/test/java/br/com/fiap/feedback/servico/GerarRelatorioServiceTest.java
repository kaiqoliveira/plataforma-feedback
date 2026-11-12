package br.com.fiap.feedback.servico;

import br.com.fiap.feedback.relatorio.GeradorRelatorioSemanal;
import br.com.fiap.feedback.relatorio.Periodo;
import br.com.fiap.feedback.suporte.Fakes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GerarRelatorioServiceTest {

    @Test
    void geraArquivaEEnviaORelatorio() {
        Fakes.RepositorioEmMemoria repo = new Fakes.RepositorioEmMemoria(
                Fakes.avaliacao("Excelente explicação", 10, "2026-11-10T12:00:00Z"),
                Fakes.avaliacao("Áudio ruim", 3, "2026-11-12T18:30:00Z"));
        Fakes.ArmazenamentoEmMemoria armazenamento = new Fakes.ArmazenamentoEmMemoria();
        Fakes.NotificadorEmMemoria notificador = new Fakes.NotificadorEmMemoria();
        GerarRelatorioService servico = new GerarRelatorioService(
                new GeradorRelatorioSemanal(repo, Fakes.RELOGIO), armazenamento, notificador);

        GerarRelatorioService.Resultado resultado = servico.gerar(Periodo.ultimos7Dias(Fakes.RELOGIO));

        assertEquals(1, armazenamento.salvos.size());
        assertEquals(2, resultado.relatorio().totalAvaliacoes());
        assertEquals("Relatório semanal de avaliações - 09/11/2026 a 15/11/2026", notificador.assuntos.get(0));

        String email = notificador.mensagens.get(0);
        assertTrue(email.contains("Média das avaliações: 6,50"));
        assertTrue(email.contains("10/11/2026 (ter.): 1") || email.contains("10/11/2026 (ter): 1"), email);
        assertTrue(email.contains("CRITICA: 1"));
        assertTrue(email.contains("Áudio ruim | CRITICA | 12/11/2026 15:30"));
        assertTrue(email.contains(resultado.urlArquivo()));
    }
}
