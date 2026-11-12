// =============================================================================
// Plataforma de Feedback - Tech Challenge Fase 4
// Infraestrutura como código (Bicep). Deploy:
//   az deployment group create -g <rg> -f infra/main.bicep -p infra/main.parameters.json
// =============================================================================

targetScope = 'resourceGroup'

@description('Região dos recursos. Padrão: a mesma do resource group.')
param location string = resourceGroup().location

@description('Prefixo dos nomes dos recursos (letras minúsculas e números).')
@minLength(3)
@maxLength(10)
param prefixo string = 'feedback'

@description('Ambiente (dev, hml, prd). Usado nos nomes e nas tags.')
@allowed(['dev', 'hml', 'prd'])
param ambiente string = 'dev'

@description('E-mails dos administradores que recebem alertas críticos e o relatório semanal.')
param emailsAdministradores array

@description('Expressão NCRONTAB (UTC) do relatório semanal. Padrão: segunda 02:59 UTC = domingo 23:59 em Brasília.')
param cronRelatorio string = '0 59 2 * * 1'

@description('Localização dos dados do Azure Communication Services (e-mail).')
param localDadosEmail string = 'Brazil'

@description('Opcional: Object ID (Entra ID) de quem vai operar a demo, para acessar o Data Explorer do Cosmos e os relatórios no Blob.')
param principalIdOperador string = ''

@description('Tags de governança aplicadas a todos os recursos.')
param tags object = {
  projeto: 'plataforma-feedback'
  ambiente: ambiente
  curso: 'fiap-adjt-fase4'
  gerenciadoPor: 'bicep'
}

var sufixo = uniqueString(resourceGroup().id)
var nomeBase = '${prefixo}-${ambiente}'
var nomeFunctionApp = 'func-${nomeBase}-${sufixo}'
var nomeStorage = take('st${prefixo}${ambiente}${sufixo}', 24)
var containerDeploy = 'deploymentpackage'
var containerRelatorios = 'relatorios'
var filaCriticas = 'avaliacoes-criticas'

// IDs de papéis internos (built-in roles) do Azure RBAC
var roles = {
  storageBlobDataOwner: 'b7e6dc6d-f1e8-4753-8033-0f276bb0955b'
  storageBlobDataReader: '2a2b9908-6ea1-4ae2-8e65-a410df84e7d1'
  storageQueueDataContributor: '974c5e8b-45b9-4653-ba55-5f855dd0fb88'
  storageTableDataContributor: '0a9a7e1f-b9d0-4cc4-a60d-0319b160aaa3'
  keyVaultSecretsUser: '4633458b-17de-408a-b874-0445c86b69e6'
}
// Papel de dados do Cosmos DB (plano de dados): "Cosmos DB Built-in Data Contributor"
var cosmosDataContributor = '00000000-0000-0000-0000-000000000002'

// -----------------------------------------------------------------------------
// Identidade: User-Assigned Managed Identity usada pela Function App.
// Criada antes da função para que as permissões já existam no primeiro start
// (inclusive a leitura do segredo no Key Vault).
// -----------------------------------------------------------------------------
resource identidade 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' = {
  name: 'id-${nomeBase}-${sufixo}'
  location: location
  tags: tags
}

// -----------------------------------------------------------------------------
// Monitoramento: Log Analytics + Application Insights
// -----------------------------------------------------------------------------
resource logAnalytics 'Microsoft.OperationalInsights/workspaces@2023-09-01' = {
  name: 'log-${nomeBase}-${sufixo}'
  location: location
  tags: tags
  properties: {
    sku: { name: 'PerGB2018' }
    retentionInDays: 30
    workspaceCapping: { dailyQuotaGb: 1 } // teto de ingestão para proteger os créditos
  }
}

resource appInsights 'Microsoft.Insights/components@2020-02-02' = {
  name: 'appi-${nomeBase}-${sufixo}'
  location: location
  tags: tags
  kind: 'web'
  properties: {
    Application_Type: 'web'
    WorkspaceResourceId: logAnalytics.id
  }
}

// -----------------------------------------------------------------------------
// Storage: host das Functions, fila de avaliações críticas e relatórios
// Sem chaves compartilhadas (apenas Entra ID), sem acesso público, TLS 1.2.
// -----------------------------------------------------------------------------
resource storage 'Microsoft.Storage/storageAccounts@2023-05-01' = {
  name: nomeStorage
  location: location
  tags: tags
  kind: 'StorageV2'
  sku: { name: 'Standard_LRS' }
  properties: {
    minimumTlsVersion: 'TLS1_2'
    supportsHttpsTrafficOnly: true
    allowBlobPublicAccess: false
    allowSharedKeyAccess: false
    defaultToOAuthAuthentication: true
    encryption: {
      keySource: 'Microsoft.Storage'
      services: {
        blob: { enabled: true }
        queue: { enabled: true, keyType: 'Account' }
      }
    }
  }
}

resource blobService 'Microsoft.Storage/storageAccounts/blobServices@2023-05-01' = {
  parent: storage
  name: 'default'
  properties: {
    deleteRetentionPolicy: { enabled: true, days: 7 }
  }
}

resource containerDeployRes 'Microsoft.Storage/storageAccounts/blobServices/containers@2023-05-01' = {
  parent: blobService
  name: containerDeploy
  properties: { publicAccess: 'None' }
}

resource containerRelatoriosRes 'Microsoft.Storage/storageAccounts/blobServices/containers@2023-05-01' = {
  parent: blobService
  name: containerRelatorios
  properties: { publicAccess: 'None' }
}

resource queueService 'Microsoft.Storage/storageAccounts/queueServices@2023-05-01' = {
  parent: storage
  name: 'default'
}

resource filaCriticasRes 'Microsoft.Storage/storageAccounts/queueServices/queues@2023-05-01' = {
  parent: queueService
  name: filaCriticas
}

resource filaPoisonRes 'Microsoft.Storage/storageAccounts/queueServices/queues@2023-05-01' = {
  parent: queueService
  name: '${filaCriticas}-poison'
}

// -----------------------------------------------------------------------------
// Banco de dados: Cosmos DB (API NoSQL) em modo SERVERLESS
// Autenticação local (chaves) desabilitada: acesso somente via RBAC do Entra ID.
// -----------------------------------------------------------------------------
resource cosmos 'Microsoft.DocumentDB/databaseAccounts@2024-05-15' = {
  name: 'cosmos-${nomeBase}-${sufixo}'
  location: location
  tags: tags
  kind: 'GlobalDocumentDB'
  properties: {
    databaseAccountOfferType: 'Standard'
    locations: [
      { locationName: location, failoverPriority: 0, isZoneRedundant: false }
    ]
    capabilities: [ { name: 'EnableServerless' } ]
    consistencyPolicy: { defaultConsistencyLevel: 'Session' }
    disableLocalAuth: true
    minimalTlsVersion: 'Tls12'
    backupPolicy: {
      type: 'Periodic'
      periodicModeProperties: {
        backupIntervalInMinutes: 240
        backupRetentionIntervalInHours: 8
        backupStorageRedundancy: 'Local'
      }
    }
  }
}

resource cosmosDb 'Microsoft.DocumentDB/databaseAccounts/sqlDatabases@2024-05-15' = {
  parent: cosmos
  name: 'feedback'
  properties: { resource: { id: 'feedback' } }
}

resource cosmosContainer 'Microsoft.DocumentDB/databaseAccounts/sqlDatabases/containers@2024-05-15' = {
  parent: cosmosDb
  name: 'avaliacoes'
  properties: {
    resource: {
      id: 'avaliacoes'
      partitionKey: { paths: [ '/diaEnvio' ], kind: 'Hash' }
      indexingPolicy: {
        indexingMode: 'consistent'
        includedPaths: [ { path: '/diaEnvio/?' }, { path: '/dataEnvio/?' }, { path: '/urgencia/?' } ]
        excludedPaths: [ { path: '/*' } ] // não indexa a descrição: menos RU por escrita
      }
    }
  }
}

resource cosmosRoleFuncao 'Microsoft.DocumentDB/databaseAccounts/sqlRoleAssignments@2024-05-15' = {
  parent: cosmos
  name: guid(cosmos.id, identidade.id, cosmosDataContributor)
  properties: {
    roleDefinitionId: '${cosmos.id}/sqlRoleDefinitions/${cosmosDataContributor}'
    principalId: identidade.properties.principalId
    scope: cosmos.id
  }
}

resource cosmosRoleOperador 'Microsoft.DocumentDB/databaseAccounts/sqlRoleAssignments@2024-05-15' = if (!empty(principalIdOperador)) {
  parent: cosmos
  name: guid(cosmos.id, principalIdOperador, cosmosDataContributor)
  properties: {
    roleDefinitionId: '${cosmos.id}/sqlRoleDefinitions/${cosmosDataContributor}'
    principalId: principalIdOperador
    scope: cosmos.id
  }
}

// -----------------------------------------------------------------------------
// E-mail: Azure Communication Services + domínio gerenciado pela Azure
// -----------------------------------------------------------------------------
resource emailService 'Microsoft.Communication/emailServices@2023-04-01' = {
  name: 'ecs-${nomeBase}-${sufixo}'
  location: 'global'
  tags: tags
  properties: { dataLocation: localDadosEmail }
}

resource emailDominio 'Microsoft.Communication/emailServices/domains@2023-04-01' = {
  parent: emailService
  name: 'AzureManagedDomain'
  location: 'global'
  tags: tags
  properties: {
    domainManagement: 'AzureManaged'
    userEngagementTracking: 'Disabled'
  }
}

resource comunicacao 'Microsoft.Communication/communicationServices@2023-04-01' = {
  name: 'acs-${nomeBase}-${sufixo}'
  location: 'global'
  tags: tags
  properties: {
    dataLocation: localDadosEmail
    linkedDomains: [ emailDominio.id ]
  }
}

// -----------------------------------------------------------------------------
// Segredos: Key Vault (RBAC). Guarda a connection string do ACS.
// -----------------------------------------------------------------------------
resource keyVault 'Microsoft.KeyVault/vaults@2023-07-01' = {
  name: take('kv-${prefixo}-${ambiente}-${sufixo}', 24)
  location: location
  tags: tags
  properties: {
    tenantId: subscription().tenantId
    sku: { family: 'A', name: 'standard' }
    enableRbacAuthorization: true
    enableSoftDelete: true
    softDeleteRetentionInDays: 7
    publicNetworkAccess: 'Enabled'
  }
}

resource segredoAcs 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'acs-connection-string'
  properties: {
    value: comunicacao.listKeys().primaryConnectionString
    contentType: 'connection-string'
  }
}

// -----------------------------------------------------------------------------
// RBAC: menor privilégio para a identidade da Function App
// -----------------------------------------------------------------------------
resource rbacBlob 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(storage.id, identidade.id, roles.storageBlobDataOwner)
  scope: storage
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', roles.storageBlobDataOwner)
    principalId: identidade.properties.principalId
    principalType: 'ServicePrincipal'
  }
}

resource rbacFila 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(storage.id, identidade.id, roles.storageQueueDataContributor)
  scope: storage
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', roles.storageQueueDataContributor)
    principalId: identidade.properties.principalId
    principalType: 'ServicePrincipal'
  }
}

resource rbacTabela 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(storage.id, identidade.id, roles.storageTableDataContributor)
  scope: storage
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', roles.storageTableDataContributor)
    principalId: identidade.properties.principalId
    principalType: 'ServicePrincipal'
  }
}

resource rbacKeyVault 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(keyVault.id, identidade.id, roles.keyVaultSecretsUser)
  scope: keyVault
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', roles.keyVaultSecretsUser)
    principalId: identidade.properties.principalId
    principalType: 'ServicePrincipal'
  }
}

resource rbacOperadorBlob 'Microsoft.Authorization/roleAssignments@2022-04-01' = if (!empty(principalIdOperador)) {
  name: guid(storage.id, principalIdOperador, roles.storageBlobDataReader)
  scope: storage
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', roles.storageBlobDataReader)
    principalId: principalIdOperador
  }
}

// -----------------------------------------------------------------------------
// Serverless: Azure Functions no plano Flex Consumption (FC1), Java 21
// -----------------------------------------------------------------------------
resource plano 'Microsoft.Web/serverfarms@2024-04-01' = {
  name: 'plan-${nomeBase}-${sufixo}'
  location: location
  tags: tags
  kind: 'functionapp'
  sku: { name: 'FC1', tier: 'FlexConsumption' }
  properties: { reserved: true } // Linux
}

resource functionApp 'Microsoft.Web/sites@2024-04-01' = {
  name: nomeFunctionApp
  location: location
  tags: union(tags, { 'azd-service-name': 'funcoes' })
  kind: 'functionapp,linux'
  identity: {
    type: 'UserAssigned'
    userAssignedIdentities: { '${identidade.id}': {} }
  }
  properties: {
    serverFarmId: plano.id
    httpsOnly: true
    keyVaultReferenceIdentity: identidade.id
    functionAppConfig: {
      deployment: {
        storage: {
          type: 'blobContainer'
          value: '${storage.properties.primaryEndpoints.blob}${containerDeploy}'
          authentication: {
            type: 'UserAssignedIdentity'
            userAssignedIdentityResourceId: identidade.id
          }
        }
      }
      scaleAndConcurrency: {
        maximumInstanceCount: 40 // teto de escala: controla custo
        instanceMemoryMB: 2048
      }
      runtime: { name: 'java', version: '21' }
    }
    siteConfig: {
      minTlsVersion: '1.2'
      ftpsState: 'Disabled'
      http20Enabled: true
      appSettings: [
        // Host das Functions usando a identidade (sem connection string)
        { name: 'AzureWebJobsStorage__accountName', value: storage.name }
        { name: 'AzureWebJobsStorage__credential', value: 'managedidentity' }
        { name: 'AzureWebJobsStorage__clientId', value: identidade.properties.clientId }
        // DefaultAzureCredential (SDK) usa esta identidade
        { name: 'AZURE_CLIENT_ID', value: identidade.properties.clientId }
        // Monitoramento
        { name: 'APPLICATIONINSIGHTS_CONNECTION_STRING', value: appInsights.properties.ConnectionString }
        // Aplicação (lidas pelo Quarkus: COSMOS_ENDPOINT -> cosmos.endpoint etc.)
        { name: 'COSMOS_ENDPOINT', value: cosmos.properties.documentEndpoint }
        { name: 'COSMOS_DATABASE', value: cosmosDb.name }
        { name: 'COSMOS_CONTAINER', value: cosmosContainer.name }
        { name: 'BLOB_ENDPOINT', value: storage.properties.primaryEndpoints.blob }
        { name: 'BLOB_CONTAINER', value: containerRelatorios }
        { name: 'EMAIL_CONNECTION_STRING', value: '@Microsoft.KeyVault(VaultName=${keyVault.name};SecretName=${segredoAcs.name})' }
        { name: 'EMAIL_REMETENTE', value: 'DoNotReply@${emailDominio.properties.mailFromSenderDomain}' }
        { name: 'EMAIL_DESTINATARIOS', value: join(emailsAdministradores, ',') }
        { name: 'RELATORIO_CRON', value: cronRelatorio }
      ]
    }
  }
  dependsOn: [
    rbacBlob
    rbacFila
    rbacTabela
    rbacKeyVault
    cosmosRoleFuncao
    containerDeployRes
    containerRelatoriosRes
    filaCriticasRes
  ]
}

// -----------------------------------------------------------------------------
// Alertas (Azure Monitor) -> Action Group com e-mail dos administradores
// -----------------------------------------------------------------------------
resource grupoAcao 'Microsoft.Insights/actionGroups@2023-01-01' = {
  name: 'ag-${nomeBase}'
  location: 'global'
  tags: tags
  properties: {
    groupShortName: 'feedbackadm'
    enabled: true
    emailReceivers: [for (email, i) in emailsAdministradores: {
      name: 'admin-${i}'
      emailAddress: email
      useCommonAlertSchema: true
    }]
  }
}

var alertas = [
  {
    nome: 'falhas-execucao'
    titulo: 'Falhas em execuções das funções'
    descricao: 'Alguma execução de função terminou com erro nos últimos 5 minutos.'
    severidade: 1
    consulta: 'requests | where cloud_RoleName =~ "${nomeFunctionApp}" and success == false'
  }
  {
    nome: 'mensagens-poison'
    titulo: 'Avaliação crítica sem notificação (poison queue)'
    descricao: 'Uma mensagem esgotou as tentativas e foi movida para a fila poison: um administrador não foi avisado.'
    severidade: 0
    consulta: 'traces | where cloud_RoleName =~ "${nomeFunctionApp}" and message has "MaxDequeueCount"'
  }
  {
    nome: 'latencia-api'
    titulo: 'Latência alta no POST /avaliacao'
    descricao: 'O p95 da função ReceberAvaliacao passou de 3 segundos.'
    severidade: 3
    consulta: 'requests | where cloud_RoleName =~ "${nomeFunctionApp}" and name == "ReceberAvaliacao" | summarize p95 = percentile(duration, 95) | where p95 > 3000'
  }
]

resource regrasAlerta 'Microsoft.Insights/scheduledQueryRules@2023-12-01' = [for a in alertas: {
  name: 'alert-${nomeBase}-${a.nome}'
  location: location
  tags: tags
  properties: {
    displayName: a.titulo
    description: a.descricao
    severity: a.severidade
    enabled: true
    evaluationFrequency: 'PT5M'
    windowSize: 'PT5M'
    scopes: [ appInsights.id ]
    criteria: {
      allOf: [
        {
          query: a.consulta
          timeAggregation: 'Count'
          operator: 'GreaterThan'
          threshold: 0
          failingPeriods: { numberOfEvaluationPeriods: 1, minFailingPeriodsToAlert: 1 }
        }
      ]
    }
    actions: { actionGroups: [ grupoAcao.id ] }
  }
}]

// -----------------------------------------------------------------------------
// Saídas
// -----------------------------------------------------------------------------
output functionAppName string = functionApp.name
output urlApi string = 'https://${functionApp.properties.defaultHostName}/avaliacao'
output cosmosEndpoint string = cosmos.properties.documentEndpoint
output storageAccount string = storage.name
output appInsightsName string = appInsights.name
output remetenteEmail string = 'DoNotReply@${emailDominio.properties.mailFromSenderDomain}'
