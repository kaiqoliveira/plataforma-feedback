package br.com.fiap.feedback.notificacao;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.models.EmailAddress;
import com.azure.communication.email.models.EmailMessage;
import com.azure.communication.email.models.EmailSendResult;
import com.azure.communication.email.models.EmailSendStatus;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;

import java.time.Duration;
import java.util.List;

/**
 * Envia e-mails pelo <b>Azure Communication Services Email</b>, usando o domínio gerenciado pela
 * Azure (DoNotReply@xxxx.azurecomm.net), criado pelo template Bicep.
 *
 * <p>A connection string do ACS fica no Azure Key Vault e chega à função por uma
 * <i>Key Vault reference</i> nas App Settings — ela nunca aparece no código nem no repositório.</p>
 */
public class EmailAcsNotificador implements Notificador {

    static final Duration TEMPO_MAXIMO_ENVIO = Duration.ofSeconds(60);

    private final EmailClient cliente;
    private final String remetente;
    private final List<String> destinatarios;

    public EmailAcsNotificador(EmailClient cliente, String remetente, List<String> destinatarios) {
        if (destinatarios.isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos um destinatário em email.destinatarios");
        }
        this.cliente = cliente;
        this.remetente = remetente;
        this.destinatarios = destinatarios;
    }

    @Override
    public void enviar(String assunto, String mensagem) {
        EmailMessage email = new EmailMessage()
                .setSenderAddress(remetente)
                .setToRecipients(destinatarios.stream().map(EmailAddress::new).toList())
                .setSubject(assunto)
                .setBodyPlainText(mensagem);

        SyncPoller<EmailSendResult, EmailSendResult> envio = cliente.beginSend(email);
        PollResponse<EmailSendResult> resposta;
        try {
            // Nunca espera indefinidamente: sem limite, a função ficaria presa até o timeout do host (5 min)
            resposta = envio.waitForCompletion(TEMPO_MAXIMO_ENVIO);
        } catch (RuntimeException e) {
            PollResponse<EmailSendResult> ultimo = envio.poll();
            throw new IllegalStateException("E-mail não confirmado pelo ACS em " + TEMPO_MAXIMO_ENVIO.toSeconds()
                    + "s. Último status: " + (ultimo == null ? "desconhecido" : ultimo.getStatus()), e);
        }
        EmailSendResult resultado = resposta.getValue();
        if (resultado == null || resultado.getStatus() != EmailSendStatus.SUCCEEDED) {
            // Lança para que a execução falhe: a fila reprocessa a mensagem e o alerta é disparado
            throw new IllegalStateException("Envio de e-mail não concluído: "
                    + (resultado == null ? "sem resposta" : resultado.getStatus()
                    + (resultado.getError() == null ? "" : " - " + resultado.getError().getMessage())));
        }
    }
}
