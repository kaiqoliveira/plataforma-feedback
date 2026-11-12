package br.com.fiap.feedback.notificacao;

/** Porta de saída para envio de notificações aos administradores. */
public interface Notificador {

    /**
     * Envia uma notificação.
     *
     * @param assunto  assunto do e-mail
     * @param mensagem corpo em texto puro
     */
    void enviar(String assunto, String mensagem);
}
