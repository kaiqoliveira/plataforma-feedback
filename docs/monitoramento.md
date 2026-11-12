# Monitoramento

## O que é coletado

A Function App envia telemetria para o **Application Insights** (workspace-based) pela App Setting `APPLICATIONINSIGHTS_CONNECTION_STRING`, criada pelo Bicep. Não é preciso mudar o código.

| Tabela | Conteúdo |
|---|---|
| `requests` | Uma linha por execução de função: nome, duração, sucesso, código HTTP |
| `traces` | Logs do `ExecutionContext` e do Quarkus (eventos de negócio estruturados) |
| `exceptions` | Exceções com stack trace |
| `dependencies` | Chamadas a Cosmos DB, Blob, Queue e ACS |

O `host.json` desativa a amostragem para `Request` e `Exception`, para que nenhuma falha deixe de ser registrada.

## Onde olhar (roteiro para o vídeo)

1. **Function App → Monitor / Invocations:** histórico de cada função, com sucesso/falha e logs;
2. **Application Insights → Live Metrics:** envie algumas avaliações e mostre as requisições em tempo real;
3. **Application Insights → Application map:** Function App → Cosmos DB / Storage / ACS;
4. **Application Insights → Failures / Performance:** taxa de erro e p95 por função;
5. **Application Insights → Logs:** rode as consultas abaixo;
6. **Monitor → Alerts → Alert rules:** os 3 alertas e o Action Group com o e-mail dos administradores.

## Consultas KQL

```kusto
// Saúde geral das funções
requests
| where timestamp > ago(24h)
| summarize execucoes = count(),
            falhas = countif(success == false),
            p50_ms = percentile(duration, 50),
            p95_ms = percentile(duration, 95) by name

// Avaliações por urgência ao longo do tempo
traces
| where message startswith "avaliacao_registrada"
| extend urgencia = extract("urgencia=(\\w+)", 1, message)
| summarize total = count() by urgencia, bin(timestamp, 1h)
| render columnchart

// Requisições rejeitadas por validação
traces
| where message startswith "avaliacao_invalida"
| project timestamp, motivo = extract("motivo=(.*)", 1, message)

// Notificações de urgência enviadas
traces
| where message startswith "notificacao_urgencia"
| project timestamp, message

// Relatórios gerados
traces
| where message startswith "relatorio_gerado"
| project timestamp, message

// Exceções por função
exceptions
| summarize total = count() by operation_Name, type, outerMessage
```

## Alertas (definidos em `infra/main.bicep`)

| Regra | Sev. | Consulta | Por quê |
|---|---|---|---|
| `alert-...-falhas-execucao` | 1 | `requests \| where success == false` | Qualquer função falhou |
| `alert-...-mensagens-poison` | 0 | `traces \| where message has "MaxDequeueCount"` | Um administrador **não** foi avisado de uma avaliação crítica |
| `alert-...-latencia-api` | 3 | p95 de `ReceberAvaliacao` > 3000 ms | Degradação da experiência do estudante |

Todas as regras avaliam a cada 5 minutos e notificam o **Action Group** `ag-feedback-dev` (e-mail dos administradores).

### Como provocar um alerta na demo

- **Falha de execução:** no portal, troque temporariamente a App Setting `COSMOS_DATABASE` para um nome inexistente e envie uma avaliação. A função falha, aparece em *Failures* e o alerta chega em até ~10 minutos. Depois, volte o valor original.
