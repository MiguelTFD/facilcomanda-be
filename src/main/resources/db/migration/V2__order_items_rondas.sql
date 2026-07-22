-- V2__order_items_rondas.sql — Feature 025 (pedidos por ronda).
-- Añade created_at y round_number a order_items para distinguir cada ronda de pedido
-- (D1/D2 de la spec): la ronda 1 son los ítems creados con la orden; cada edición que
-- agrega cantidad neta genera la ronda siguiente con su propia hora.
-- Backfill de filas históricas: pertenecen a la ronda 1 y su hora es la de la orden padre.

ALTER TABLE public.order_items ADD COLUMN created_at timestamp(6) without time zone;
ALTER TABLE public.order_items ADD COLUMN round_number integer;

UPDATE public.order_items oi
SET round_number = 1,
    created_at = COALESCE(
        (SELECT o.order_date FROM public.orders o WHERE o.id = oi.order_id),
        now())
WHERE oi.round_number IS NULL OR oi.created_at IS NULL;

ALTER TABLE public.order_items ALTER COLUMN round_number SET NOT NULL;
ALTER TABLE public.order_items ALTER COLUMN created_at SET NOT NULL;
