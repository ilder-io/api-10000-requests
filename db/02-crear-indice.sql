-- Arreglo E1: índice en la columna por la que buscamos.
-- Antes y después, compara:
--   EXPLAIN ANALYZE SELECT * FROM pedidos WHERE codigo = 'PED-500000';
CREATE INDEX idx_pedidos_codigo ON pedidos (codigo);
