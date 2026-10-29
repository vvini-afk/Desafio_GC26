-- Campos opcionais para não exigir valores para cidades já cadastradas.
-- IF NOT EXISTS permite aplicar a migration caso as colunas tenham sido
-- criadas manualmente antes do versionamento pelo Flyway.
ALTER TABLE public.cidade
    ADD COLUMN IF NOT EXISTS regiao character varying,
    ADD COLUMN IF NOT EXISTS multiplicador numeric(10, 4);
