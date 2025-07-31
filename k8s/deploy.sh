
#!/bin/bash

# Cores para output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}Iniciando deploy da aplicação Remessa API${NC}"

# Verificar se o kubectl está instalado
if ! command -v kubectl &> /dev/null; then
    echo -e "${RED}kubectl não está instalado. Por favor, instale o kubectl primeiro.${NC}"
    exit 1
fi

# Verificar se o Docker está rodando
if ! docker info &> /dev/null; then
    echo -e "${RED}Docker não está rodando. Por favor, inicie o Docker primeiro.${NC}"
    exit 1
fi

echo -e "${GREEN}1. Construindo imagem Docker...${NC}"
docker build -t remessa-api:latest .

# Verificar se o build foi bem sucedido
if [ $? -eq 0 ]; then
    echo -e "${GREEN}Imagem Docker construída com sucesso${NC}"
else
    echo -e "${RED}Falha ao construir imagem Docker${NC}"
    exit 1
fi

echo -e "${GREEN}2. Aplicando configurações Kubernetes...${NC}"

# Aplicar ConfigMap
echo "Aplicando ConfigMap..."
kubectl apply -f k8s/configmap.yaml
if [ $? -ne 0 ]; then
    echo -e "${RED}Falha ao aplicar ConfigMap${NC}"
    exit 1
fi

# Aplicar Deployment
echo "Aplicando Deployment..."
kubectl apply -f k8s/deployment.yaml
if [ $? -ne 0 ]; then
    echo -e "${RED}Falha ao aplicar Deployment${NC}"
    exit 1
fi

# Aplicar Service
echo "Aplicando Service..."
kubectl apply -f k8s/service.yaml
if [ $? -ne 0 ]; then
    echo -e "${RED}Falha ao aplicar Service${NC}"
    exit 1
fi

echo -e "${GREEN}3. Verificando status do deployment...${NC}"
kubectl rollout status deployment/remessa-api

# Mostrar informações úteis
echo -e "${GREEN}Deploy concluído!${NC}"
echo -e "${GREEN}Para verificar os pods:${NC} kubectl get pods"
echo -e "${GREEN}Para verificar o serviço:${NC} kubectl get services"
echo -e "${GREEN}Para verificar os logs:${NC} kubectl logs -l app=remessa-api"

# Pegando o IP do serviço (pode variar dependendo do ambiente)
echo -e "${GREEN}Aguardando IP do serviço...${NC}"
sleep 5
kubectl get service remessa-api-service