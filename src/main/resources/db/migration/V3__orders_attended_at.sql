-- V3__orders_attended_at.sql — Feature 027 (mesero: comanda atendida).
-- Guarda el momento del último "ATENDIDO" marcado por el MESERO sobre la orden.
-- El estado "atendida" en sí se persiste en orders.status = 'DELIVERED' (decisión 6 de
-- roles.md); esta columna existe para responder una pregunta distinta: qué rondas de
-- pedido llegaron DESPUÉS del último atendido, para que la pantalla de cocina las
-- resalte cuando la comanda se reactiva.
--
-- Migración aditiva y sin backfill: NULL significa "nunca atendida", que es exactamente
-- la situación de todas las órdenes históricas. No se toca ninguna fila existente.

ALTER TABLE public.orders ADD COLUMN attended_at timestamp(6) without time zone;
