package br.com.fiap.feedback.armazenamento;

import br.com.fiap.feedback.relatorio.RelatorioSemanal;

/** Porta de saída para arquivar o relatório gerado. */
public interface ArmazenamentoRelatorio {

    /**
     * Salva o relatório em JSON.
     *
     * @return URL do arquivo salvo
     */
    String salvar(RelatorioSemanal relatorio);
}
