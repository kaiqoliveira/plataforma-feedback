package br.com.fiap.feedback.servico;

import br.com.fiap.feedback.armazenamento.ArmazenamentoRelatorio;
import br.com.fiap.feedback.notificacao.Notificador;
import br.com.fiap.feedback.relatorio.FormatadorRelatorio;
import br.com.fiap.feedback.relatorio.GeradorRelatorioSemanal;
import br.com.fiap.feedback.relatorio.Periodo;
import br.com.fiap.feedback.relatorio.RelatorioSemanal;
import jakarta.enterprise.context.ApplicationScoped;

/** Caso de uso: gerar o relatório semanal, arquivá-lo e enviá-lo aos administradores. */
@ApplicationScoped
public class GerarRelatorioService {

    private final GeradorRelatorioSemanal gerador;
    private final ArmazenamentoRelatorio armazenamento;
    private final Notificador notificador;

    public GerarRelatorioService(GeradorRelatorioSemanal gerador, ArmazenamentoRelatorio armazenamento,
                                 Notificador notificador) {
        this.gerador = gerador;
        this.armazenamento = armazenamento;
        this.notificador = notificador;
    }

    public Resultado gerar(Periodo periodo) {
        RelatorioSemanal relatorio = gerador.gerar(periodo);
        String url = armazenamento.salvar(relatorio);
        notificador.enviar(FormatadorRelatorio.assunto(relatorio), FormatadorRelatorio.texto(relatorio, url));
        return new Resultado(relatorio, url);
    }

    /** Relatório gerado e URL do arquivo no Blob Storage. */
    public record Resultado(RelatorioSemanal relatorio, String urlArquivo) {
    }
}
