package br.com.fiap.feedback.funcoes;

import br.com.fiap.feedback.api.MensagemAvaliacaoCritica;
import br.com.fiap.feedback.dominio.Avaliacao;
import br.com.fiap.feedback.servico.NotificarUrgenciaService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.QueueTrigger;
import jakarta.inject.Inject;

/**
 * <b>Função 2 — NotificarUrgencia</b>
 *
 * <p><b>Responsabilidade única:</b> enviar e-mail aos administradores para cada avaliação crítica
 * (descrição, urgência e data de envio).</p>
 *
 * <ul>
 *   <li>Gatilho: Azure Storage Queue {@code avaliacoes-criticas};</li>
 *   <li>Resiliência: se o envio falhar, a função lança exceção e o host reprocessa a mensagem
 *       (até {@code maxDequeueCount}, definido no host.json). Esgotadas as tentativas, a mensagem vai
 *       para {@code avaliacoes-criticas-poison} e um alerta do Azure Monitor avisa a equipe.</li>
 * </ul>
 */
public class NotificarUrgenciaFunction {

    @Inject
    NotificarUrgenciaService servico;

    @Inject
    ObjectMapper mapper;

    @FunctionName("NotificarUrgencia")
    public void executar(
            @QueueTrigger(name = "mensagem", queueName = ReceberAvaliacaoFunction.FILA_CRITICAS,
                    connection = "AzureWebJobsStorage")
            String mensagem,
            final ExecutionContext contexto) throws JsonProcessingException {

        Avaliacao avaliacao = mapper.readValue(mensagem, MensagemAvaliacaoCritica.class).paraAvaliacao();
        boolean enviada = servico.notificar(avaliacao);
        contexto.getLogger().info("notificacao_urgencia id=%s enviada=%s".formatted(avaliacao.id(), enviada));
    }
}
