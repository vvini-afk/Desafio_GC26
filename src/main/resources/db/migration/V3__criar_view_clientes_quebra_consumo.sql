CREATE OR REPLACE VIEW public.vw_clientes_quebra_consumo AS
WITH pedidos_ordenados AS (
    SELECT
        p.id_cliente,
        p.data_pedido::date AS data_pedido,
        LAG(p.data_pedido::date) OVER (
            PARTITION BY p.id_cliente
            ORDER BY p.data_pedido
        ) AS pedido_anterior
    FROM public.pedido p
    WHERE p.id_cliente IS NOT NULL
),
cadencia AS (
    SELECT
        id_cliente,
        AVG((data_pedido - pedido_anterior)::numeric) AS intervalo_medio_dias,
        COUNT(pedido_anterior) AS intervalos_observados
    FROM pedidos_ordenados
    GROUP BY id_cliente
    HAVING COUNT(pedido_anterior) >= 2
),
ultima_compra_historico AS (
    SELECT
        id_cliente,
        MAX(data_pedido::date) AS ultima_compra
    FROM public.pedido
    WHERE id_cliente IS NOT NULL
    GROUP BY id_cliente
),
clientes_com_dias AS (
    SELECT
        c.id_cliente,
        c.nome,
        COALESCE(c.ultima_compra::date, u.ultima_compra) AS ultima_compra,
        cad.intervalo_medio_dias,
        cad.intervalos_observados,
        CURRENT_DATE - COALESCE(c.ultima_compra::date, u.ultima_compra) AS dias_sem_comprar
    FROM public.cliente c
    INNER JOIN cadencia cad ON cad.id_cliente = c.id_cliente
    INNER JOIN ultima_compra_historico u ON u.id_cliente = c.id_cliente
)
SELECT
    id_cliente,
    nome,
    ultima_compra,
    dias_sem_comprar,
    intervalo_medio_dias,
    intervalos_observados
FROM clientes_com_dias
WHERE intervalo_medio_dias > 0
  AND dias_sem_comprar > intervalo_medio_dias * 1.5;
