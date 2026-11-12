package br.com.fiap.feedback.dominio;

import br.com.fiap.feedback.config.Fuso;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Avaliação de uma aula enviada por um estudante.
 *
 * @param id        identificador único (UUID)
 * @param descricao comentário do estudante
 * @param nota      nota de 0 a 10
 * @param urgencia  urgência derivada da nota
 * @param dataEnvio instante em que a avaliação foi recebida (UTC)
 */
public record Avaliacao(String id, String descricao, int nota, Urgencia urgencia, Instant dataEnvio) {

    /** Tamanho máximo aceito para a descrição. */
    public static final int TAMANHO_MAXIMO_DESCRICAO = 2000;

    public Avaliacao {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(urgencia, "urgencia");
        Objects.requireNonNull(dataEnvio, "dataEnvio");
        validarDescricao(descricao);
    }

    /**
     * Cria uma nova avaliação validando as regras de negócio e calculando a urgência.
     *
     * @param descricao comentário do estudante (obrigatório, até 2000 caracteres)
     * @param nota      nota (obrigatória, 0 a 10)
     * @param relogio   relógio usado para carimbar a data de envio
     * @return avaliação pronta para ser persistida
     * @throws AvaliacaoInvalidaException se algum dado for inválido
     */
    public static Avaliacao nova(String descricao, Integer nota, Clock relogio) {
        if (nota == null) {
            throw new AvaliacaoInvalidaException("nota é obrigatória");
        }
        String descricaoNormalizada = descricao == null ? null : descricao.strip();
        return new Avaliacao(
                UUID.randomUUID().toString(),
                descricaoNormalizada,
                nota,
                Urgencia.daNota(nota),
                relogio.instant());
    }

    /** Dia (no fuso de São Paulo) em que a avaliação foi enviada. Usado como chave do índice por data. */
    public LocalDate diaEnvio() {
        return dataEnvio.atZone(Fuso.SAO_PAULO).toLocalDate();
    }

    private static void validarDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            throw new AvaliacaoInvalidaException("descricao é obrigatória");
        }
        if (descricao.length() > TAMANHO_MAXIMO_DESCRICAO) {
            throw new AvaliacaoInvalidaException(
                    "descricao deve ter no máximo " + TAMANHO_MAXIMO_DESCRICAO + " caracteres");
        }
    }
}
