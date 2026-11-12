# Documentação das funções serverless

As três funções ficam na mesma Function App (mesmo deploy), mas cada uma tem **gatilho, responsabilidade e escala próprios**. A Azure cria instâncias separadas sob demanda para cada tipo de gatilho.

---

## 1. `ReceberAvaliacao`

| | |
|---|---|
| **Classe** | `funcoes/ReceberAvaliacaoFunction.java` |
| **Gatilho** | HTTP `POST /avaliacao` |
| **Autenticação** | `AuthorizationLevel.FUNCTION`: header `x-functions-key` |
| **Entradas** | Corpo JSON `{"descricao": string, "nota": int 0..10}` |
| **Saídas** | Documento no Cosmos DB; mensagem na fila `avaliacoes-criticas` (só se `CRITICA`); resposta HTTP |
| **Responsabilidade** | Validar, classificar a urgência e registrar a avaliação |

**Regras de validação**

| Campo | Regra | Erro (HTTP 400) |
|---|---|---|
| corpo | obrigatório e JSON válido | `corpo da requisição é obrigatório` / `JSON inválido...` |
| `descricao` | obrigatória, não vazia, até 2000 caracteres | `descricao é obrigatória` / `descricao deve ter no máximo 2000 caracteres` |
| `nota` | obrigatória, inteira, de 0 a 10 | `nota é obrigatória` / `nota deve estar entre 0 e 10` |

**Respostas**

| Status | Quando |
|---|---|
| `201 Created` | Avaliação registrada. Corpo: `id`, `descricao`, `nota`, `urgencia`, `dataEnvio` |
| `400 Bad Request` | Dados inválidos. Corpo: `{"erro": "..."}` |
| `401 Unauthorized` | Sem `x-functions-key` ou chave inválida |
| `500` | Falha de infraestrutura. Fica registrada no Application Insights e dispara alerta |

**Documento gravado (Cosmos DB, container `avaliacoes`)**

```json
{
  "id": "9b2f3c1e-...",
  "descricao": "O vídeo da aula não carrega",
  "nota": 1,
  "urgencia": "CRITICA",
  "dataEnvio": "2026-11-12T17:42:00Z",
  "diaEnvio": "2026-11-12"
}
```

`diaEnvio` é a **chave de partição**, calculada no fuso de São Paulo: uma avaliação às 23h de Brasília conta para o dia certo no relatório.

---

## 2. `NotificarUrgencia`

| | |
|---|---|
| **Classe** | `funcoes/NotificarUrgenciaFunction.java` |
| **Gatilho** | Azure Storage Queue `avaliacoes-criticas` |
| **Entrada** | Mensagem JSON `MensagemAvaliacaoCritica` (id, descricao, nota, urgencia, dataEnvio) |
| **Saída** | E-mail aos administradores (SMTP via `quarkus-mailer`) |
| **Responsabilidade** | Avisar os administradores sobre cada avaliação crítica |

**Conteúdo do e-mail** (dados exigidos pelo enunciado):

```
Assunto: [URGENTE] Avaliação crítica recebida (nota 1)

Descrição: O vídeo da aula não carrega
Urgência: CRITICA
Data de envio: 12/11/2026 14:42 (horário de Brasília)
```

**Resiliência**
- Se o envio do e-mail falhar, a função lança exceção e o host **reprocessa** a mensagem (`maxDequeueCount: 5` no `host.json`);
- Depois de 5 falhas, a mensagem vai para `avaliacoes-criticas-poison`, e o alerta **"Avaliação crítica sem notificação"** (severidade 0) dispara;
- A avaliação continua salva no banco mesmo se o e-mail falhar.

---

## 3. `GerarRelatorioSemanal`

| | |
|---|---|
| **Classe** | `funcoes/GerarRelatorioSemanalFunction.java` |
| **Gatilho** | Timer, NCRONTAB da App Setting `RELATORIO_CRON` |
| **Agendamento padrão** | `0 59 2 * * 1` (UTC) = **domingo 23:59 em Brasília** |
| **Entradas** | Avaliações dos últimos 7 dias (Cosmos DB, uma consulta por intervalo de partições) |
| **Saídas** | JSON no Blob `relatorios/<ano>/relatorio-<inicio>_a_<fim>.json`; e-mail com o resumo |
| **Responsabilidade** | Consolidar e entregar o relatório semanal |

**Conteúdo do relatório** (dados exigidos pelo enunciado):

- **Média das avaliações** (2 casas decimais);
- **Quantidade de avaliações por dia** (os 7 dias, inclusive os zerados);
- **Quantidade de avaliações por urgência** (CRITICA, MEDIA, BAIXA);
- Lista com **descrição, urgência e data de envio** de cada avaliação.

**Exemplo de e-mail**

```
RELATÓRIO SEMANAL DE AVALIAÇÕES
Período: 09/11/2026 a 15/11/2026

RESUMO
  Total de avaliações: 2
  Média das avaliações: 6,50

QUANTIDADE DE AVALIAÇÕES POR DIA
  09/11/2026 (seg.): 0
  10/11/2026 (ter.): 1
  ...

QUANTIDADE DE AVALIAÇÕES POR URGÊNCIA
  CRITICA: 1
  MEDIA:   0
  BAIXA:   1

AVALIAÇÕES (descrição | urgência | data de envio)
  - Excelente explicação | BAIXA | 10/11/2026 09:00
  - Áudio ruim | CRITICA | 12/11/2026 15:30
```

**Execução manual** (demonstração): portal → Function App → `GerarRelatorioSemanal` → *Test/Run*, ou

```bash
curl -X POST "https://<app>.azurewebsites.net/admin/functions/GerarRelatorioSemanal" \
  -H "x-functions-key: <master key>" -H "Content-Type: application/json" -d '{"input": ""}'
```

Fora do agendamento, o período cobre os 7 dias terminando **hoje**, então as avaliações feitas durante a demo aparecem no relatório.

---

## Configuração (App Settings)

| Setting | Propriedade Quarkus | Descrição |
|---|---|---|
| `COSMOS_ENDPOINT` | `cosmos.endpoint` | Endpoint da conta Cosmos |
| `COSMOS_DATABASE` / `COSMOS_CONTAINER` | `cosmos.database` / `cosmos.container` | `feedback` / `avaliacoes` |
| `COSMOS_KEY` | `cosmos.key` | **Só local** (emulador) |
| `BLOB_ENDPOINT` / `BLOB_CONTAINER` | `blob.endpoint` / `blob.container` | Conta e container dos relatórios |
| `BLOB_CONNECTION_STRING` | `blob.connection-string` | **Só local** (Azurite) |
| `EMAIL_PROVEDOR` | `email.provedor` | `smtp` (padrão no deploy com Gmail), `acs` ou `log` |
| `QUARKUS_MAILER_USERNAME` / `QUARKUS_MAILER_FROM` | `quarkus.mailer.*` | Conta Gmail remetente |
| `QUARKUS_MAILER_PASSWORD` | `quarkus.mailer.password` | Key Vault reference (senha de app) |
| `EMAIL_CONNECTION_STRING` | `email.connection-string` | Key Vault reference para o ACS (alternativa) |
| `EMAIL_REMETENTE` | `email.remetente` | `DoNotReply@<dominio>.azurecomm.net` |
| `EMAIL_DESTINATARIOS` | `email.destinatarios` | Lista separada por vírgula |
| `RELATORIO_CRON` | (binding) | Agendamento do relatório |
| `AzureWebJobsStorage__*` | (host) | Storage do host via Managed Identity |
| `AZURE_CLIENT_ID` | (SDK) | Identidade usada pelo `DefaultAzureCredential` |
| `APPLICATIONINSIGHTS_CONNECTION_STRING` | (host) | Telemetria |
