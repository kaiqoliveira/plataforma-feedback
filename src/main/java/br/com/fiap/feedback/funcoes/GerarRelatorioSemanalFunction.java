package br.com.fiap.feedback.funcoes;

import br.com.fiap.feedback.relatorio.Periodo;
import br.com.fiap.feedback.servico.GerarRelatorioService;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import jakarta.inject.Inject;

import java.time.Clock;

/**
 * <b>Função 3 — GerarRelatorioSemanal</b>
 *
 * <p><b>Responsabilidade única:</b> gerar o relatório semanal (média das avaliações, quantidade por dia
 * e por urgência, lista com descrição/urgência/data), salvá-lo no Blob Storage e enviá-lo por e-mail.</p>
 *
 * <ul>
 *   <li>Gatilho: Timer, expressão NCRONTAB vinda da App Setting {@code RELATORIO_CRON}.
 *       Padrão: {@code 0 59 2 * * 1} (UTC) = domingo 23:59 em Brasília;</li>
 *   <li>Execução manual (demonstração): {@code POST /admin/functions/GerarRelatorioSemanal}
 *       com a master key, ou botão "Test/Run" no portal.</li>
 * </ul>
 */
public class GerarRelatorioSemanalFunction {

    @Inject
    GerarRelatorioService servico;

    @Inject
    Clock relogio;

    @FunctionName("GerarRelatorioSemanal")
    public void executar(
            @TimerTrigger(name = "agendamento", schedule = "%RELATORIO_CRON%") String agendamento,
            final ExecutionContext contexto) {

        GerarRelatorioService.Resultado resultado = servico.gerar(Periodo.ultimos7Dias(relogio));

        contexto.getLogger().info("relatorio_gerado inicio=%s fim=%s total=%d media=%s arquivo=%s".formatted(
                resultado.relatorio().inicio(),
                resultado.relatorio().fim(),
                resultado.relatorio().totalAvaliacoes(),
                resultado.relatorio().mediaNotas(),
                resultado.urlArquivo()));
    }
}
