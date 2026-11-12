#!/usr/bin/env bash
# =============================================================================
# Configuração inicial (executar UMA vez, com o Azure CLI logado: az login)
#
#  1. Cria o resource group
#  2. Cria uma App Registration para o GitHub Actions com credencial federada (OIDC)
#  3. Concede ao pipeline apenas o necessário, no escopo do resource group:
#       - Contributor: criar/atualizar recursos
#       - Role Based Access Control Administrator: criar as atribuições RBAC do Bicep
#  4. Mostra os valores a cadastrar no GitHub (Secrets e Variables)
#
# Uso: ./scripts/configurar-azure.sh <usuario-ou-org>/<repositorio> [resource-group] [regiao]
# Ex.: ./scripts/configurar-azure.sh meu-grupo/plataforma-feedback rg-feedback-dev brazilsouth
# =============================================================================
set -euo pipefail

REPO="${1:?Informe o repositório no formato owner/repo}"
RG="${2:-rg-feedback-dev}"
REGIAO="${3:-brazilsouth}"
NOME_APP="github-${REPO//\//-}-deploy"

ASSINATURA=$(az account show --query id -o tsv)
TENANT=$(az account show --query tenantId -o tsv)

echo ">> Registrando provedores de recursos"
for p in Microsoft.Web Microsoft.DocumentDB Microsoft.Storage Microsoft.KeyVault \
         Microsoft.Insights Microsoft.OperationalInsights Microsoft.Communication \
         Microsoft.ManagedIdentity Microsoft.AlertsManagement; do
  az provider register --namespace "$p" --output none
done

echo ">> Criando resource group $RG em $REGIAO"
az group create --name "$RG" --location "$REGIAO" \
  --tags projeto=plataforma-feedback ambiente=dev curso=fiap-adjt-fase4 --output none
RG_ID=$(az group show --name "$RG" --query id -o tsv)

echo ">> Criando App Registration $NOME_APP"
APP_ID=$(az ad app create --display-name "$NOME_APP" --query appId -o tsv)
az ad sp create --id "$APP_ID" --output none 2>/dev/null || true
SP_ID=$(az ad sp show --id "$APP_ID" --query id -o tsv)

echo ">> Criando credenciais federadas (OIDC) para o GitHub"
for SUBJECT in "repo:${REPO}:environment:dev" "repo:${REPO}:ref:refs/heads/main"; do
  NOME=$(echo "$SUBJECT" | tr ':/' '--' | cut -c1-120)
  az ad app federated-credential create --id "$APP_ID" --parameters "{
    \"name\": \"${NOME}\",
    \"issuer\": \"https://token.actions.githubusercontent.com\",
    \"subject\": \"${SUBJECT}\",
    \"audiences\": [\"api://AzureADTokenExchange\"]
  }" --output none 2>/dev/null || echo "   (credencial ${SUBJECT} já existe)"
done

echo ">> Concedendo papéis no escopo do resource group (menor privilégio)"
az role assignment create --assignee-object-id "$SP_ID" --assignee-principal-type ServicePrincipal \
  --role "Contributor" --scope "$RG_ID" --output none
az role assignment create --assignee-object-id "$SP_ID" --assignee-principal-type ServicePrincipal \
  --role "Role Based Access Control Administrator" --scope "$RG_ID" --output none

MEU_ID=$(az ad signed-in-user show --query id -o tsv 2>/dev/null || echo "")

cat <<EOF

=============================================================================
Cadastre no GitHub (Settings > Secrets and variables > Actions):

  Secrets:
    AZURE_CLIENT_ID       = ${APP_ID}
    AZURE_TENANT_ID       = ${TENANT}
    AZURE_SUBSCRIPTION_ID = ${ASSINATURA}

  Variables:
    AZURE_RESOURCE_GROUP   = ${RG}
    EMAILS_ADMINISTRADORES = ["seu-email@exemplo.com"]

Crie também o environment "dev" (Settings > Environments).

Para acessar o Data Explorer do Cosmos e os relatórios no Blob durante a demo,
coloque seu Object ID em infra/main.parameters.json (principalIdOperador):
    ${MEU_ID:-<execute: az ad signed-in-user show --query id -o tsv>}
=============================================================================
EOF
