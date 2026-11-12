package br.com.fiap.feedback.servico;

import br.com.fiap.feedback.dominio.Avaliacao;
import br.com.fiap.feedback.notificacao.MensagemUrgencia;
import br.com.fiap.feedback.notificacao.Notificador;
import jakarta.enterprise.context.ApplicationScoped;

/** Caso de uso: avisar os administradores sobre uma avaliação crítica. */
@ApplicationScoped
public class NotificarUrgenciaService {

    private final Notificador notificador;

    public NotificarUrgenciaService(Notificador notificador) {
        this.notificador = notificador;
    }

    /**
     * Envia o aviso se a avaliação for crítica.
     *
     * @return {@code true} se o e-mail foi enviado; {@code false} se a avaliação não era crítica
     */
    public boolean notificar(Avaliacao avaliacao) {
        if (!avaliacao.urgencia().isCritica()) {
            return false; // defesa em profundidade: a fila só deveria conter itens críticos
        }
        notificador.enviar(MensagemUrgencia.assunto(avaliacao), MensagemUrgencia.corpo(avaliacao));
        return true;
    }
}
