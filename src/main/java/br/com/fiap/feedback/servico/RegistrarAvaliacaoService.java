package br.com.fiap.feedback.servico;

import br.com.fiap.feedback.api.AvaliacaoRequest;
import br.com.fiap.feedback.dominio.Avaliacao;
import br.com.fiap.feedback.dominio.AvaliacaoInvalidaException;
import br.com.fiap.feedback.persistencia.AvaliacaoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.time.Clock;
import java.util.Set;
import java.util.stream.Collectors;

/** Caso de uso: validar, classificar a urgência e persistir uma nova avaliação. */
@ApplicationScoped
public class RegistrarAvaliacaoService {

    private final AvaliacaoRepository repositorio;
    private final Validator validador;
    private final Clock relogio;

    public RegistrarAvaliacaoService(AvaliacaoRepository repositorio, Validator validador, Clock relogio) {
        this.repositorio = repositorio;
        this.validador = validador;
        this.relogio = relogio;
    }

    /**
     * @return a avaliação persistida (com id, urgência e data de envio)
     * @throws AvaliacaoInvalidaException se a requisição violar alguma regra
     */
    public Avaliacao registrar(AvaliacaoRequest requisicao) {
        if (requisicao == null) {
            throw new AvaliacaoInvalidaException("corpo da requisição é obrigatório");
        }
        Set<ConstraintViolation<AvaliacaoRequest>> violacoes = validador.validate(requisicao);
        if (!violacoes.isEmpty()) {
            throw new AvaliacaoInvalidaException(violacoes.stream()
                    .map(ConstraintViolation::getMessage)
                    .sorted()
                    .collect(Collectors.joining("; ")));
        }
        Avaliacao avaliacao = Avaliacao.nova(requisicao.descricao(), requisicao.nota(), relogio);
        repositorio.salvar(avaliacao);
        return avaliacao;
    }
}
