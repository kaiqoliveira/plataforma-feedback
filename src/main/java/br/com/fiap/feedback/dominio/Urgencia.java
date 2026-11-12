package br.com.fiap.feedback.dominio;

/**
 * Nível de urgência de uma avaliação, derivado da nota (0 a 10).
 *
 * <ul>
 *   <li>{@link #CRITICA}: nota 0 a 3 — dispara notificação imediata aos administradores;</li>
 *   <li>{@link #MEDIA}: nota 4 a 6 — entra no relatório semanal;</li>
 *   <li>{@link #BAIXA}: nota 7 a 10 — entra no relatório semanal.</li>
 * </ul>
 */
public enum Urgencia {

    CRITICA(0, 3),
    MEDIA(4, 6),
    BAIXA(7, 10);

    private final int notaMinima;
    private final int notaMaxima;

    Urgencia(int notaMinima, int notaMaxima) {
        this.notaMinima = notaMinima;
        this.notaMaxima = notaMaxima;
    }

    /**
     * Classifica a nota na faixa de urgência correspondente.
     *
     * @param nota nota de 0 a 10
     * @return urgência correspondente
     * @throws AvaliacaoInvalidaException se a nota estiver fora do intervalo
     */
    public static Urgencia daNota(int nota) {
        for (Urgencia u : values()) {
            if (nota >= u.notaMinima && nota <= u.notaMaxima) {
                return u;
            }
        }
        throw new AvaliacaoInvalidaException("nota deve estar entre 0 e 10");
    }

    public boolean isCritica() {
        return this == CRITICA;
    }
}
