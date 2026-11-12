package br.com.fiap.feedback.notificacao;

import org.jboss.logging.Logger;

/**
 * Notificador usado em desenvolvimento local (quando {@code email.connection-string} não está
 * configurada): apenas registra o e-mail no log, sem depender do Azure Communication Services.
 */
public class LogNotificador implements Notificador {

    private static final Logger LOG = Logger.getLogger(LogNotificador.class);

    @Override
    public void enviar(String assunto, String mensagem) {
        LOG.infof("[E-MAIL SIMULADO] Assunto: %s%n%s", assunto, mensagem);
    }
}
