package br.com.fiap.feedback.funcoes;

import br.com.fiap.feedback.api.AvaliacaoRequest;
import br.com.fiap.feedback.api.AvaliacaoResponse;
import br.com.fiap.feedback.api.MensagemAvaliacaoCritica;
import br.com.fiap.feedback.dominio.Avaliacao;
import br.com.fiap.feedback.dominio.AvaliacaoInvalidaException;
import br.com.fiap.feedback.servico.RegistrarAvaliacaoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.functions.annotation.QueueOutput;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.Optional;

/**
 * <b>Função 1 — ReceberAvaliacao</b>
 *
 * <p><b>Responsabilidade única:</b> receber a avaliação via HTTP, validar, persistir no Cosmos DB e,
 * se for crítica, publicar um evento na fila {@code avaliacoes-criticas}.</p>
 *
 * <p>Ela <b>não</b> envia e-mails: quem faz isso é a {@link NotificarUrgenciaFunction}, disparada
 * pela fila. Assim a API responde rápido e uma falha no e-mail não faz o estudante perder a avaliação.</p>
 *
 * <ul>
 *   <li>Gatilho: HTTP {@code POST /avaliacao} (nível {@code FUNCTION}: exige o header {@code x-functions-key});</li>
 *   <li>Saída: binding de fila (Azure Storage Queue) — escrito apenas quando a urgência é CRITICA.</li>
 * </ul>
 */
public class ReceberAvaliacaoFunction {

    public static final String FILA_CRITICAS = "avaliacoes-criticas";

    @Inject
    RegistrarAvaliacaoService servico;

    @Inject
    ObjectMapper mapper;

    @FunctionName("ReceberAvaliacao")
    public HttpResponseMessage executar(
            @HttpTrigger(name = "requisicao", methods = {HttpMethod.POST}, route = "avaliacao",
                    authLevel = AuthorizationLevel.FUNCTION)
            HttpRequestMessage<Optional<String>> requisicao,
            @QueueOutput(name = "filaCriticas", queueName = FILA_CRITICAS, connection = "AzureWebJobsStorage")
            OutputBinding<String> filaCriticas,
            final ExecutionContext contexto) {

        try {
            AvaliacaoRequest corpo = lerCorpo(requisicao.getBody().orElse(null));
            Avaliacao avaliacao = servico.registrar(corpo);

            if (avaliacao.urgencia().isCritica()) {
                filaCriticas.setValue(escrever(MensagemAvaliacaoCritica.de(avaliacao)));
            }
            contexto.getLogger().info("avaliacao_registrada id=%s urgencia=%s"
                    .formatted(avaliacao.id(), avaliacao.urgencia()));

            return json(requisicao, HttpStatus.CREATED, AvaliacaoResponse.de(avaliacao));
        } catch (AvaliacaoInvalidaException e) {
            contexto.getLogger().warning("avaliacao_invalida motivo=" + e.getMessage());
            return json(requisicao, HttpStatus.BAD_REQUEST, Map.of("erro", e.getMessage()));
        }
        // Demais exceções (ex.: Cosmos indisponível) sobem: a execução fica como falha no
        // Application Insights, o host responde 500 sem detalhes internos e o alerta é disparado.
    }

    private AvaliacaoRequest lerCorpo(String corpo) {
        if (corpo == null || corpo.isBlank()) {
            throw new AvaliacaoInvalidaException("corpo da requisição é obrigatório");
        }
        try {
            return mapper.readValue(corpo, AvaliacaoRequest.class);
        } catch (JsonProcessingException e) {
            throw new AvaliacaoInvalidaException("JSON inválido: esperado {\"descricao\": string, \"nota\": inteiro de 0 a 10}");
        }
    }

    private HttpResponseMessage json(HttpRequestMessage<?> requisicao, HttpStatus status, Object corpo) {
        return requisicao.createResponseBuilder(status)
                .header("Content-Type", "application/json; charset=utf-8")
                .body(escrever(corpo))
                .build();
    }

    /** Serializa para JSON. Falha aqui é erro de programação (não de entrada), por isso vira unchecked. */
    private String escrever(Object objeto) {
        try {
            return mapper.writeValueAsString(objeto);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar JSON", e);
        }
    }
}
