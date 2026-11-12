package br.com.fiap.feedback.persistencia;

import br.com.fiap.feedback.dominio.Avaliacao;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;

/**
 * Repositório sobre o Azure Cosmos DB (API NoSQL, modo <b>serverless</b>: paga-se por requisição,
 * sem capacidade provisionada — adequado ao volume do desafio e aos créditos limitados).
 */
@ApplicationScoped
public class CosmosAvaliacaoRepository implements AvaliacaoRepository {

    static final String CONSULTA_PERIODO =
            "SELECT * FROM c WHERE c.diaEnvio >= @inicio AND c.diaEnvio <= @fim ORDER BY c.dataEnvio";

    private final CosmosContainer container;

    public CosmosAvaliacaoRepository(CosmosContainer container) {
        this.container = container;
    }

    @Override
    public void salvar(Avaliacao avaliacao) {
        AvaliacaoDocumento documento = AvaliacaoDocumento.de(avaliacao);
        // createItem (e não upsert): um id repetido gera 409 em vez de sobrescrever a avaliação
        container.createItem(documento, new PartitionKey(documento.diaEnvio()), new CosmosItemRequestOptions());
    }

    @Override
    public List<Avaliacao> buscarPorPeriodo(LocalDate inicio, LocalDate fim) {
        SqlQuerySpec consulta = new SqlQuerySpec(CONSULTA_PERIODO, List.of(
                new SqlParameter("@inicio", inicio.toString()),
                new SqlParameter("@fim", fim.toString())));
        return container.queryItems(consulta, new CosmosQueryRequestOptions(), AvaliacaoDocumento.class)
                .stream()
                .map(AvaliacaoDocumento::paraDominio)
                .toList();
    }
}
