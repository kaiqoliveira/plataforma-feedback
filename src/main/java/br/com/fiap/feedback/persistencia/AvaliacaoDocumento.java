package br.com.fiap.feedback.persistencia;

import br.com.fiap.feedback.dominio.Avaliacao;
import br.com.fiap.feedback.dominio.Urgencia;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * Documento armazenado no container {@code avaliacoes} do Cosmos DB.
 *
 * <p>Chave de partição: {@code /diaEnvio} (yyyy-MM-dd, fuso de São Paulo). Como o relatório consulta
 * por intervalo de dias, a consulta toca apenas as partições da semana.</p>
 *
 * @param id        UUID da avaliação (também é o id do documento)
 * @param dataEnvio instante ISO-8601 em UTC
 * @param diaEnvio  dia do envio no fuso de São Paulo (chave de partição)
 */
@JsonIgnoreProperties(ignoreUnknown = true) // ignora metadados do Cosmos (_rid, _etag, _ts...)
public record AvaliacaoDocumento(
        String id, String descricao, int nota, String urgencia, String dataEnvio, String diaEnvio) {

    public static AvaliacaoDocumento de(Avaliacao a) {
        return new AvaliacaoDocumento(
                a.id(), a.descricao(), a.nota(), a.urgencia().name(),
                a.dataEnvio().toString(), a.diaEnvio().toString());
    }

    public Avaliacao paraDominio() {
        return new Avaliacao(id, descricao, nota, Urgencia.valueOf(urgencia), Instant.parse(dataEnvio));
    }
}
