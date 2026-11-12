package br.com.fiap.feedback.api;

import br.com.fiap.feedback.config.JsonConfig;
import br.com.fiap.feedback.dominio.Avaliacao;
import br.com.fiap.feedback.persistencia.AvaliacaoDocumento;
import br.com.fiap.feedback.suporte.Fakes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContratosJsonTest {

    private final ObjectMapper mapper = JsonConfig.configurar(new ObjectMapper());
    private final Avaliacao avaliacao = Fakes.avaliacao("Aula travou", 1, "2026-11-13T01:30:00Z");

    @Test
    void leCorpoDoPostEIgnoraCamposExtras() throws Exception {
        AvaliacaoRequest r = mapper.readValue("{\"descricao\":\"Boa\",\"nota\":8,\"extra\":true}", AvaliacaoRequest.class);
        assertEquals("Boa", r.descricao());
        assertEquals(8, r.nota());
    }

    @Test
    void notaAusenteViraNulaParaSerValidada() throws Exception {
        assertNull(mapper.readValue("{\"descricao\":\"Boa\"}", AvaliacaoRequest.class).nota());
    }

    @Test
    void rejeitaNotaDecimal() {
        assertThrows(JsonProcessingException.class,
                () -> mapper.readValue("{\"descricao\":\"Boa\",\"nota\":7.5}", AvaliacaoRequest.class));
    }

    @Test
    void mensagemDaFilaFazIdaEVoltaSemPerda() throws Exception {
        String json = mapper.writeValueAsString(MensagemAvaliacaoCritica.de(avaliacao));
        assertTrue(json.contains("\"dataEnvio\":\"2026-11-13T01:30:00Z\""), json);
        assertEquals(avaliacao, mapper.readValue(json, MensagemAvaliacaoCritica.class).paraAvaliacao());
    }

    @Test
    void documentoCosmosUsaDiaDeSaoPauloComoParticao() throws Exception {
        AvaliacaoDocumento doc = AvaliacaoDocumento.de(avaliacao);
        assertEquals("2026-11-12", doc.diaEnvio(), "01:30 UTC do dia 13 ainda é dia 12 em São Paulo");

        String comMetadados = mapper.writeValueAsString(doc).replace("}", ",\"_rid\":\"x\",\"_etag\":\"y\"}");
        assertEquals(avaliacao, mapper.readValue(comMetadados, AvaliacaoDocumento.class).paraDominio());
    }
}
