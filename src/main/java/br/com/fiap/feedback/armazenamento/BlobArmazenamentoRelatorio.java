package br.com.fiap.feedback.armazenamento;

import br.com.fiap.feedback.relatorio.RelatorioSemanal;
import com.azure.core.util.BinaryData;
import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Arquiva o relatório em um container privado do Azure Blob Storage. O acesso público está
 * desabilitado na conta e a autenticação é feita pela Managed Identity da Function App.
 */
@ApplicationScoped
public class BlobArmazenamentoRelatorio implements ArmazenamentoRelatorio {

    private final BlobContainerClient container;
    private final ObjectMapper mapper;

    public BlobArmazenamentoRelatorio(BlobContainerClient container, ObjectMapper mapper) {
        this.container = container;
        this.mapper = mapper;
    }

    @Override
    public String salvar(RelatorioSemanal relatorio) {
        BlobClient blob = container.getBlobClient(nomeArquivo(relatorio));
        BlobParallelUploadOptions opcoes = new BlobParallelUploadOptions(BinaryData.fromString(json(relatorio)))
                .setHeaders(new BlobHttpHeaders().setContentType("application/json; charset=utf-8"));
        blob.uploadWithResponse(opcoes, null, Context.NONE);
        return blob.getBlobUrl();
    }

    /** Ex.: {@code 2026/relatorio-2026-11-09_a_2026-11-15.json}. */
    public static String nomeArquivo(RelatorioSemanal r) {
        return "%d/relatorio-%s_a_%s.json".formatted(r.fim().getYear(), r.inicio(), r.fim());
    }

    private String json(RelatorioSemanal relatorio) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(relatorio);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar relatório", e);
        }
    }
}
