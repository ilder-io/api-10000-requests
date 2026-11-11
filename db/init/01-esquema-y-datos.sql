-- Datos FICTICIOS para el laboratorio. Ningún nombre ni cifra es real.

CREATE TABLE productos (
    id      INT PRIMARY KEY,
    nombre  TEXT          NOT NULL,
    precio  NUMERIC(10,2) NOT NULL,
    activo  BOOLEAN       NOT NULL
);

INSERT INTO productos (id, nombre, precio, activo)
SELECT g, 'Producto ' || g, round((random() * 200 + 5)::numeric, 2), true
FROM generate_series(1, 500) AS g;

CREATE TABLE pedidos (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(20)   NOT NULL,   -- ¡sin índice! (error E1 de la demo)
    cliente     VARCHAR(40)   NOT NULL,
    producto_id INT           NOT NULL REFERENCES productos(id),
    cantidad    INT           NOT NULL,
    total       NUMERIC(10,2) NOT NULL,
    estado      VARCHAR(20)   NOT NULL,
    creado_en   TIMESTAMP     NOT NULL
);

INSERT INTO pedidos (codigo, cliente, producto_id, cantidad, total, estado, creado_en)
SELECT 'PED-' || g,
       'Cliente ' || (g % 50000),
       (g % 500) + 1,
       (g % 5) + 1,
       round((random() * 500 + 10)::numeric, 2),
       (ARRAY['CREADO', 'PAGADO', 'ENVIADO', 'ENTREGADO'])[(g % 4) + 1],
       now() - (g % 365) * interval '1 day'
FROM generate_series(1, 1000000) AS g;

ANALYZE;
