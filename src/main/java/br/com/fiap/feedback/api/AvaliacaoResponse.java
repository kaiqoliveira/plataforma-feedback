package br.com.fiap.feedback.api;

import br.com.fiap.feedback.dominio.Avaliacao;

import java.time.Instant;

/** Resposta do {@code POST /avaliacao} (HTTP 201 Created). */
public record AvaliacaoResponse(String id, String descricao, int nota, String urgencia, Instant dataEnvio) {

    public static AvaliacaoResponse de(Avaliacao a) {
        return new AvaliacaoResponse(a.id(), a.descricao(), a.nota(), a.urgencia().name(), a.dataEnvio());
    }
}
