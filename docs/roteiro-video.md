# Roteiro do vídeo de demonstração (~10 min)

> Antes de gravar: deploy feito, e-mail de administrador confirmado, `requests.http` preenchido com URL e chave, e o portal aberto no resource group.

## 1. Introdução e modelo de cloud (1 min 30)
- Problema: feedback das aulas e acompanhamento rápido da satisfação.
- Modelo escolhido: **nuvem pública Azure**, **serverless (FaaS)** com Azure Functions Flex Consumption + **PaaS** gerenciados (Cosmos DB serverless, Storage, ACS, Key Vault, App Insights).
- Por que serverless: paga por execução, escala até zero e não há servidor para administrar.
- Mostrar o **diagrama de arquitetura** do README.

## 2. Código e responsabilidade única (2 min)
- Estrutura de pacotes: `funcoes` → `servico` → `dominio`/`relatorio` ← adapters (`persistencia`, `notificacao`, `armazenamento`).
- Abrir as 3 funções e mostrar que cada uma é fina e tem um único gatilho e uma única responsabilidade.
- Quarkus: CDI (`@Inject`), Bean Validation no `AvaliacaoRequest`, produtores dos clientes Azure em `AzureProducers` com `DefaultAzureCredential`.
- Rodar `mvn verify` (testes passando).

## 3. Infraestrutura e segurança (2 min)
- `infra/main.bicep`: recursos e **tags**.
- Portal → resource group: listar os recursos.
- Segurança:
  - Cosmos DB → *Keys*: autenticação por chave **desabilitada**;
  - Storage → *Configuration*: *Allow storage account key access* = **Disabled**, acesso público aos blobs desabilitado, TLS 1.2;
  - Managed Identity → *Azure role assignments*: papéis mínimos;
  - Key Vault → segredo do ACS; Function App → *Environment variables*: `EMAIL_CONNECTION_STRING` como **Key Vault reference** (ícone verde).

## 4. Deploy automatizado (1 min)
- `.github/workflows/deploy.yml`: build → testes → login **OIDC** (sem senha) → Bicep → publicação das funções.
- GitHub → Actions: mostrar uma execução verde.
- (Opcional) Fazer uma pequena alteração, dar push e mostrar o pipeline rodando.

## 5. Aplicação funcionando (2 min 30)
1. `POST /avaliacao` com nota **9** → 201, `BAIXA`.
2. `POST /avaliacao` com nota **5** → 201, `MEDIA`.
3. `POST /avaliacao` com nota **1** → 201, `CRITICA` → abrir o **e-mail de urgência** (descrição, urgência e data de envio).
4. `POST` inválido (`nota: 11`) → 400 com mensagem; sem chave → 401.
5. Cosmos DB → Data Explorer: documentos gravados com `diaEnvio`.
6. Storage → Queues: `avaliacoes-criticas` (consumida) e `-poison` vazia.
7. Disparar **`GerarRelatorioSemanal`** (*Test/Run* ou `requests.http`, item 6) → abrir o **e-mail do relatório** (média, quantidade por dia e por urgência) e o **JSON** no container `relatorios`.

## 6. Monitoramento (1 min)
- Application Insights → **Live Metrics** enquanto envia avaliações.
- **Logs** → rodar a consulta "Saúde geral das funções" e "Avaliações por urgência" (gráfico).
- **Alerts** → as 3 regras + Action Group. (Opcional: mostrar um alerta disparado.)

## 7. Encerramento (30 s)
- Recapitular os requisitos atendidos (tabela abaixo) e mostrar o link do repositório.

| Requisito do enunciado | Onde está |
|---|---|
| Ambiente cloud com segurança e governança de acesso | Bicep + Managed Identity + RBAC + Key Vault + chaves desabilitadas |
| Componentes de suporte (banco etc.) | Cosmos DB, Storage Queue/Blob, ACS, Key Vault |
| Deploy automatizado | GitHub Actions (`deploy.yml`) com OIDC |
| Aplicação monitorada | Application Insights + Log Analytics + alertas |
| Notificação automática de problemas críticos | `NotificarUrgencia` + alertas do Azure Monitor |
| Relatório semanal com média | `GerarRelatorioSemanal` (timer) |
| Mínimo de 2 funções serverless com responsabilidade única | 3 funções: HTTP, Queue, Timer |
