package br.com.fiap.feedback.notificacao;

import br.com.fiap.feedback.config.Fuso;
import br.com.fiap.feedback.dominio.Avaliacao;

/**
 * Monta o e-mail de aviso de urgência com os dados exigidos pelo enunciado:
 * descrição, urgência e data de envio.
 */
public final class MensagemUrgencia {

    private MensagemUrgencia() {
    }

    public static String assunto(Avaliacao a) {
        return "[URGENTE] Avaliação crítica recebida (nota " + a.nota() + ")";
    }

    public static String corpo(Avaliacao a) {
        return """
                Uma avaliação crítica foi recebida na Plataforma de Feedback.

                Descrição: %s
                Urgência: %s
                Data de envio: %s (horário de Brasília)

                Nota: %d
                ID da avaliação: %s

                Recomenda-se contato com o estudante e revisão da aula avaliada.
                """.formatted(
                a.descricao(),
                a.urgencia().name(),
                Fuso.DATA_HORA_BR.format(a.dataEnvio()),
                a.nota(),
                a.id());
    }
}
