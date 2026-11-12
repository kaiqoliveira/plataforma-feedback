package br.com.fiap.feedback.dominio;

/** Lançada quando os dados de uma avaliação não respeitam as regras de negócio. */
public class AvaliacaoInvalidaException extends RuntimeException {

    public AvaliacaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
