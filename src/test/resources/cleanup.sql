-- Desabilitar verificação de foreign keys temporariamente
SET REFERENTIAL_INTEGRITY FALSE;

-- Limpar todas as tabelas
TRUNCATE TABLE remessas;
TRUNCATE TABLE transacoes_diarias;
TRUNCATE TABLE carteiras;
TRUNCATE TABLE usuarios;

-- Reabilitar verificação de foreign keys
SET REFERENTIAL_INTEGRITY TRUE;