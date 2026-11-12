package br.com.fiap.feedback.notificacao;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.models.EmailAddress;
import com.azure.communication.email.models.EmailMessage;
import com.azure.communication.email.models.EmailSendResult;
import com.azure.communication.email.models.EmailSendStatus;
import com.azure.core.util.polling.PollResponse;

import java.util.List;

/**
 * Envia e-mails pelo <b>Azure Communication Services Email</b>, usando o domínio gerenciado pela
 * Azure (DoNotReply@xxxx.azurecomm.net), criado pelo template Bicep.
 *
 * <p>A connection string do ACS fica no Azure Key Vault e chega à função por uma
 * <i>Key Vault reference</i> nas App Settings — ela nunca aparece no código nem no repositório.</p>
 */
public class EmailAcsNotificador implements Notificador {

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

        PollResponse<EmailSendResult> resposta = cliente.beginSend(email).waitForCompletion();
        EmailSendResult resultado = resposta.getValue();
        if (resultado == null || resultado.getStatus() != EmailSendStatus.SUCCEEDED) {
            // Lança para que a execução falhe: a fila reprocessa a mensagem e o alerta é disparado
            throw new IllegalStateException("Envio de e-mail não concluído: "
                    + (resultado == null ? "sem resposta" : resultado.getStatus()));
        }
    }
}
