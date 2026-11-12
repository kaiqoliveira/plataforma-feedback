package br.com.fiap.feedback.suporte;

import br.com.fiap.feedback.armazenamento.ArmazenamentoRelatorio;
import br.com.fiap.feedback.dominio.Avaliacao;
import br.com.fiap.feedback.dominio.Urgencia;
import br.com.fiap.feedback.notificacao.Notificador;
import br.com.fiap.feedback.persistencia.AvaliacaoRepository;
import br.com.fiap.feedback.relatorio.RelatorioSemanal;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/** Implementações em memória das portas, para testes unitários sem Azure. */
public final class Fakes {

    /** 2026-11-15 (domingo) 23:59 em São Paulo. */
    public static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-11-16T02:59:00Z"), ZoneOffset.UTC);

    private Fakes() {
    }

    public static Avaliacao avaliacao(String descricao, int nota, String instante) {
        return new Avaliacao("id-" + descricao, descricao, nota, Urgencia.daNota(nota), Instant.parse(instante));
    }

    public static class RepositorioEmMemoria implements AvaliacaoRepository {
        public final List<Avaliacao> avaliacoes = new ArrayList<>();
        public final List<LocalDate[]> consultas = new ArrayList<>();

        public RepositorioEmMemoria(Avaliacao... iniciais) {
            avaliacoes.addAll(List.of(iniciais));
        }

        @Override
        public void salvar(Avaliacao avaliacao) {
            avaliacoes.add(avaliacao);
        }

        @Override
        public List<Avaliacao> buscarPorPeriodo(LocalDate inicio, LocalDate fim) {
            consultas.add(new LocalDate[]{inicio, fim});
            return avaliacoes.stream()
                    .filter(a -> !a.diaEnvio().isBefore(inicio) && !a.diaEnvio().isAfter(fim))
                    .toList();
        }
    }

    public static class NotificadorEmMemoria implements Notificador {
        public final List<String> assuntos = new ArrayList<>();
        public final List<String> mensagens = new ArrayList<>();

        @Override
        public void enviar(String assunto, String mensagem) {
            assuntos.add(assunto);
            mensagens.add(mensagem);
        }
    }

    public static class ArmazenamentoEmMemoria implements ArmazenamentoRelatorio {
        public final List<RelatorioSemanal> salvos = new ArrayList<>();

        @Override
        public String salvar(RelatorioSemanal relatorio) {
            salvos.add(relatorio);
            return "https://conta.blob.core.windows.net/relatorios/teste.json";
        }
    }
}
