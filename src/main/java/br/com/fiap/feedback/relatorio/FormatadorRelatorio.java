package br.com.fiap.feedback.relatorio;

import br.com.fiap.feedback.dominio.Urgencia;
import br.com.fiap.feedback.config.Fuso;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;

/** Converte o relatório em texto legível para o e-mail semanal. */
public final class FormatadorRelatorio {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    /** Limite de avaliações listadas no e-mail; a lista completa fica no JSON salvo no Blob Storage. */
    static final int MAXIMO_ITENS_EMAIL = 200;

    private FormatadorRelatorio() {
    }

    public static String assunto(RelatorioSemanal r) {
        return "Relatório semanal de avaliações - %s a %s".formatted(
                Fuso.DATA_BR.format(r.inicio()), Fuso.DATA_BR.format(r.fim()));
    }

    public static String texto(RelatorioSemanal r, String urlArquivo) {
        StringBuilder sb = new StringBuilder();
        sb.append("RELATÓRIO SEMANAL DE AVALIAÇÕES\n");
        sb.append("Período: ").append(Fuso.DATA_BR.format(r.inicio()))
                .append(" a ").append(Fuso.DATA_BR.format(r.fim())).append('\n');
        sb.append("Gerado em: ").append(Fuso.DATA_HORA_BR.format(r.geradoEm())).append(" (horário de Brasília)\n\n");

        sb.append("RESUMO\n");
        sb.append("  Total de avaliações: ").append(r.totalAvaliacoes()).append('\n');
        sb.append("  Média das avaliações: ")
                .append(r.mediaNotas() == null ? "sem avaliações no período" : String.format(PT_BR, "%.2f", r.mediaNotas()))
                .append('\n');

        sb.append("\nQUANTIDADE DE AVALIAÇÕES POR DIA\n");
        for (Map.Entry<LocalDate, Long> e : r.quantidadePorDia().entrySet()) {
            String diaSemana = e.getKey().getDayOfWeek().getDisplayName(TextStyle.SHORT, PT_BR);
            sb.append(String.format("  %s (%s): %d%n", Fuso.DATA_BR.format(e.getKey()), diaSemana, e.getValue()));
        }

        sb.append("\nQUANTIDADE DE AVALIAÇÕES POR URGÊNCIA\n");
        for (Map.Entry<Urgencia, Long> e : r.quantidadePorUrgencia().entrySet()) {
            sb.append(String.format("  %-8s %d%n", e.getKey().name() + ":", e.getValue()));
        }

        sb.append("\nAVALIAÇÕES (descrição | urgência | data de envio)\n");
        if (r.avaliacoes().isEmpty()) {
            sb.append("  Nenhuma avaliação no período.\n");
        }
        r.avaliacoes().stream().limit(MAXIMO_ITENS_EMAIL).forEach(i ->
                sb.append("  - ").append(i.descricao().replace('\n', ' '))
                        .append(" | ").append(i.urgencia().name())
                        .append(" | ").append(Fuso.DATA_HORA_BR.format(i.dataEnvio()))
                        .append('\n'));
        if (r.avaliacoes().size() > MAXIMO_ITENS_EMAIL) {
            sb.append("  ... e mais ").append(r.avaliacoes().size() - MAXIMO_ITENS_EMAIL)
                    .append(" avaliações (veja o relatório completo).\n");
        }

        sb.append("\nRelatório completo (JSON no Blob Storage): ").append(urlArquivo).append('\n');
        return sb.toString();
    }
}
