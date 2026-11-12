package br.com.fiap.feedback.notificacao;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;

import java.util.List;

/**
 * Envia e-mails por SMTP usando a extensão <b>quarkus-mailer</b> (servidor do Gmail, porta 587 com STARTTLS).
 *
 * <p>Usuário e remetente vêm das App Settings; a senha (senha de app do Google) fica no Azure Key Vault
 * e chega à função por <i>Key Vault reference</i> — nunca aparece no código nem no repositório.</p>
 */
public class SmtpNotificador implements Notificador {

    private final Mailer mailer;
    private final List<String> destinatarios;

    public SmtpNotificador(Mailer mailer, List<String> destinatarios) {
        if (destinatarios.isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos um destinatário em email.destinatarios");
        }
        this.mailer = mailer;
        this.destinatarios = destinatarios;
    }

    @Override
    public void enviar(String assunto, String mensagem) {
        // Envio síncrono: se o SMTP recusar, a exceção sobe e a execução falha (fila reprocessa + alerta)
        mailer.send(new Mail()
                .setTo(destinatarios)
                .setSubject(assunto)
                .setText(mensagem));
    }
}
