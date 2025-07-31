
@echo off
echo Iniciando deploy da aplicacao Remessa API

REM Verificar se o Docker esta rodando
docker info > nul 2>&1
if errorlevel 1 (
    echo Docker nao esta rodando. Por favor, inicie o Docker primeiro.
    exit /b 1
)

echo 1. Construindo imagem Docker...
docker build -t remessa-api:latest .
if errorlevel 1 (
    echo Falha ao construir imagem Docker
    exit /b 1
)

echo 2. Aplicando configuracoes Kubernetes...

echo Aplicando ConfigMap...
kubectl apply -f k8s/configmap.yaml
if errorlevel 1 (
    echo Falha ao aplicar ConfigMap
    exit /b 1
)

echo Aplicando Deployment...
kubectl apply -f k8s/deployment.yaml
if errorlevel 1 (
    echo Falha ao aplicar Deployment
    exit /b 1
)

echo Aplicando Service...
kubectl apply -f k8s/service.yaml
if errorlevel 1 (
    echo Falha ao aplicar Service
    exit /b 1
)

echo 3. Verificando status do deployment...
kubectl rollout status deployment/remessa-api

echo Deploy concluido!
echo Para verificar os pods: kubectl get pods
echo Para verificar o servico: kubectl get services
echo Para verificar os logs: kubectl logs -l app=remessa-api

echo Aguardando IP do servico...
timeout /t 5 /nobreak > nul
kubectl get service remessa-api-service