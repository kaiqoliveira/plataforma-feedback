package br.com.fiap.feedback.api;

import br.com.fiap.feedback.dominio.Avaliacao;
import br.com.fiap.feedback.dominio.Urgencia;

import java.time.Instant;

/**
 * Contrato da mensagem publicada na fila {@code avaliacoes-criticas} (Azure Storage Queue).
 * É o "evento" que desacopla a função que recebe avaliações da função que notifica administradores.
 */
public record MensagemAvaliacaoCritica(String id, String descricao, int nota, Urgencia urgencia, Instant dataEnvio) {

    public static MensagemAvaliacaoCritica de(Avaliacao a) {
        return new MensagemAvaliacaoCritica(a.id(), a.descricao(), a.nota(), a.urgencia(), a.dataEnvio());
    }

    public Avaliacao paraAvaliacao() {
        return new Avaliacao(id, descricao, nota, urgencia, dataEnvio);
    }
}
