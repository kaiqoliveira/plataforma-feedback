package br.com.fiap.feedback.config;

import br.com.fiap.feedback.notificacao.EmailAcsNotificador;
import br.com.fiap.feedback.notificacao.LogNotificador;
import br.com.fiap.feedback.notificacao.Notificador;
import br.com.fiap.feedback.notificacao.SmtpNotificador;
import io.quarkus.mailer.Mailer;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.core.credential.TokenCredential;
import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

/**
 * Produtores CDI dos clientes Azure. Cada cliente é criado uma única vez por instância da
 * Function App e reaproveitado entre execuções (conexões HTTP reutilizadas, menos latência).
 *
 * <p><b>Segurança:</b> em produção não há chaves nem senhas — Cosmos DB e Storage são acessados com a
 * <i>Managed Identity</i> da Function App ({@link com.azure.identity.DefaultAzureCredential}) e
 * permissões RBAC mínimas definidas no Bicep. As alternativas com chave/connection string existem
 * apenas para desenvolvimento local (emuladores).</p>
 */
@Singleton
public class AzureProducers {

    private static final Logger LOG = Logger.getLogger(AzureProducers.class);

    @Produces
    @Singleton
    Clock relogio() {
        return Clock.systemUTC();
    }

    @Produces
    @Singleton
    TokenCredential credencial() {
        return new DefaultAzureCredentialBuilder().build();
    }

    @Produces
    @Singleton
    CosmosContainer containerAvaliacoes(
            TokenCredential credencial,
            @ConfigProperty(name = "cosmos.endpoint") String endpoint,
            @ConfigProperty(name = "cosmos.key") Optional<String> chaveLocal,
            @ConfigProperty(name = "cosmos.database") String banco,
            @ConfigProperty(name = "cosmos.container") String container) {

        CosmosClientBuilder builder = new CosmosClientBuilder()
                .endpoint(endpoint)
                .consistencyLevel(ConsistencyLevel.SESSION)
                // Modo gateway (HTTPS 443): mais simples em ambiente serverless e compatível com o emulador
                .gatewayMode()
                .contentResponseOnWriteEnabled(false);

        chaveLocal.filter(k -> !k.isBlank()).ifPresentOrElse(builder::key, () -> builder.credential(credencial));

        CosmosClient cliente = builder.buildClient();
        return cliente.getDatabase(banco).getContainer(container);
    }

    @Produces
    @Singleton
    BlobContainerClient containerRelatorios(
            TokenCredential credencial,
            @ConfigProperty(name = "blob.endpoint") Optional<String> endpoint,
            @ConfigProperty(name = "blob.connection-string") Optional<String> connectionStringLocal,
            @ConfigProperty(name = "blob.container") String container) {

        BlobServiceClientBuilder builder = new BlobServiceClientBuilder();
        Optional<String> cs = connectionStringLocal.filter(s -> !s.isBlank());
        if (cs.isPresent()) {
            builder.connectionString(cs.get());
        } else {
            builder.endpoint(endpoint.orElseThrow(() -> new IllegalStateException("Configure BLOB_ENDPOINT")))
                    .credential(credencial);
        }
        return builder.buildClient().getBlobContainerClient(container);
    }

    /**
     * Escolhe o provedor de e-mail pela propriedade {@code email.provedor} (App Setting {@code EMAIL_PROVEDOR}):
     * {@code smtp} (Gmail via quarkus-mailer), {@code acs} (Azure Communication Services) ou {@code log}.
     */
    @Produces
    @Singleton
    Notificador notificador(
            Mailer mailer,
            @ConfigProperty(name = "email.provedor", defaultValue = "log") String provedor,
            @ConfigProperty(name = "email.connection-string") Optional<String> connectionString,
            @ConfigProperty(name = "email.remetente") Optional<String> remetente,
            @ConfigProperty(name = "email.destinatarios") Optional<List<String>> destinatarios) {

        List<String> para = destinatarios.orElse(List.of());
        switch (provedor.trim().toLowerCase()) {
            case "smtp":
                LOG.info("Provedor de e-mail: SMTP (quarkus-mailer)");
                return new SmtpNotificador(mailer, para);
            case "acs":
                LOG.info("Provedor de e-mail: Azure Communication Services");
                return new EmailAcsNotificador(
                        new EmailClientBuilder().connectionString(connectionString
                                .filter(s -> !s.isBlank())
                                .orElseThrow(() -> new IllegalStateException("Configure EMAIL_CONNECTION_STRING")))
                                .buildClient(),
                        remetente.orElseThrow(() -> new IllegalStateException("Configure EMAIL_REMETENTE")),
                        para);
            default:
                LOG.warn("email.provedor=" + provedor + ": e-mails serão apenas registrados no log");
                return new LogNotificador();
        }
    }
}
