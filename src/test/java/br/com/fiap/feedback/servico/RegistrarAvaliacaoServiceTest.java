package br.com.fiap.feedback.servico;

import br.com.fiap.feedback.api.AvaliacaoRequest;
import br.com.fiap.feedback.dominio.Avaliacao;
import br.com.fiap.feedback.dominio.AvaliacaoInvalidaException;
import br.com.fiap.feedback.dominio.Urgencia;
import br.com.fiap.feedback.suporte.Fakes;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrarAvaliacaoServiceTest {

    private static final Validator VALIDADOR = Validation.byDefaultProvider().configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator();

    private final Fakes.RepositorioEmMemoria repositorio = new Fakes.RepositorioEmMemoria();
    private final RegistrarAvaliacaoService servico =
            new RegistrarAvaliacaoService(repositorio, VALIDADOR, Fakes.RELOGIO);

    @Test
    void persisteAvaliacaoComUrgenciaCalculada() {
        Avaliacao a = servico.registrar(new AvaliacaoRequest("Aula sem áudio", 1));

        assertEquals(Urgencia.CRITICA, a.urgencia());
        assertEquals(Instant.parse("2026-11-16T02:59:00Z"), a.dataEnvio());
        assertEquals(1, repositorio.avaliacoes.size());
    }

    @Test
    void rejeitaRequisicaoInvalidaSemPersistir() {
        AvaliacaoInvalidaException e = assertThrows(AvaliacaoInvalidaException.class,
                () -> servico.registrar(new AvaliacaoRequest("  ", 11)));

        assertTrue(e.getMessage().contains("descricao é obrigatória"));
        assertTrue(e.getMessage().contains("nota deve estar entre 0 e 10"));
        assertTrue(repositorio.avaliacoes.isEmpty());
    }

    @Test
    void rejeitaNotaAusenteECorpoNulo() {
        assertThrows(AvaliacaoInvalidaException.class, () -> servico.registrar(new AvaliacaoRequest("ok", null)));
        assertThrows(AvaliacaoInvalidaException.class, () -> servico.registrar(null));
    }
}
