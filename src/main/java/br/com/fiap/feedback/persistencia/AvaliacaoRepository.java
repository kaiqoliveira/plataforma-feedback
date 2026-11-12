package br.com.fiap.feedback.persistencia;

import br.com.fiap.feedback.dominio.Avaliacao;

import java.time.LocalDate;
import java.util.List;

/** Porta de persistência de avaliações. */
public interface AvaliacaoRepository {

    /** Persiste uma nova avaliação. */
    void salvar(Avaliacao avaliacao);

    /**
     * Busca as avaliações enviadas entre dois dias (inclusive), no fuso de São Paulo.
     *
     * @param inicio primeiro dia
     * @param fim    último dia
     */
    List<Avaliacao> buscarPorPeriodo(LocalDate inicio, LocalDate fim);
}
