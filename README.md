# Tu API funciona… hasta que llegan 10 000 requests

Laboratorio del vídeo **«Tu API funciona… hasta que llegan 10 000 requests (cómo encontrar el cuello de botella)»** del canal de YouTube de **Ilder Tocto**.

Es una API de pedidos **ficticia** con tres errores de rendimiento puestos a propósito. La llevas a ~10 000 requests por minuto con k6, ves cómo se cae, encuentras el cuello de botella con las métricas y la arreglas **sin agregar servidores**.

> Todo corre en una sola PC y sin Docker. Las cifras absolutas dependen de tu máquina: lo que importa es **dónde se rompe** y cómo cambia al arreglarlo. Ningún dato es real.

## Qué hay dentro

| Pieza | Puerto | Qué es |
|---|---|---|
| `db-embebida` | 5432 | PostgreSQL 17 real, embebido desde Java ([zonky embedded-postgres](https://github.com/zonkyio/embedded-postgres)), con 1 000 000 de pedidos ficticios y una consola SQL |
| `envios/EnviosServer.java` | 8081 | Servicio de envíos ficticio con un retraso configurable |
| `pedidos-api` | 8080 | La API: Spring Boot 3.5, Java 21, JdbcTemplate, HikariCP |
| Actuator | 9090 | Métricas en otro puerto, con sus propios hilos: responde aunque la API esté saturada |
| `k6/carga.js` | — | Prueba de carga: sube a 167 req/s (~10 000 por minuto) y la sostiene |
| `herramientas/metricas.mjs` | — | Panel de métricas en la terminal, una línea por segundo |
| `docker-compose.yml` | — | Alternativa con Docker para la base de datos (opcional) |

## Requisitos
- Java 21 y Maven 3.8+
- Node.js 18+ (para el panel de métricas)
- [k6](https://grafana.com/docs/k6/latest/set-up/install-k6/). En Windows sin permisos de administrador sirve el `.zip` de las [releases](https://github.com/grafana/k6/releases): deja `k6.exe` en `herramientas/`.
- Unos 2 GB de RAM libres

## Compilar
```bash
cd pedidos-api && mvn -q -DskipTests package && cd ..
cd db-embebida && mvn -q -DskipTests package && cd ..
```

## Arrancar (desde esta carpeta, cada cosa en su terminal)
```bash
# 1 · Base de datos. La primera vez genera el millón de pedidos (unos segundos o minutos)
java -Xmx128m -jar db-embebida/target/db-embebida.jar
```
```bash
# 2 · Servicio de envíos
java -Xmx128m envios/EnviosServer.java
```
```bash
# 3 · La API
java -Xmx384m -jar pedidos-api/target/pedidos-api.jar
```
```bash
# 4 · Panel de métricas
node herramientas/metricas.mjs
```
```bash
# 5 · La prueba de carga
k6 run k6/carga.js
```

## Los errores y cómo arreglarlos

| Paso | Qué haces | Qué deberías ver |
|---|---|---|
| **0 · Roto** | Estado inicial: sin índice, sin timeout, sin caché | Conexiones 10/10, una cola larga esperando conexión, latencia de segundos |
| **1 · ¿Más servidores?** | Segunda instancia: `java -Xmx384m -jar pedidos-api/target/pedidos-api.jar --server.port=8082 --management.server.port=9092` y `k6 run -e URLS=http://localhost:8080,http://localhost:8082 k6/carga.js`. Panel: `PUERTO_API=8082 PUERTO_ACTUATOR=9092 node herramientas/metricas.mjs` | Mejora poco: las dos instancias esperan a la misma base de datos |
| **2 · Índice** | En la consola SQL: `CREATE INDEX idx_pedidos_codigo ON pedidos (codigo);` | De `Seq Scan` a `Index Scan`. La latencia cae a milisegundos |
| **3 · Dependencia lenta** | `curl -X POST "localhost:8081/admin/retraso?ms=2000"` y vuelve a lanzar k6 | Hilos de Tomcat 200/200, unas 100 req/s como máximo. Prueba `curl -m 5 localhost:8080/api/ping`: tampoco responde |
| **3b · Timeout** | En `pedidos-api/src/main/resources/application.yml`: `lab.envios.timeout-ms: 300`, recompila y reinicia | Unos 50 hilos ocupados. El pedido llega con `"envio": "NO_DISPONIBLE"` |
| **4 · Caché** | En el mismo archivo: `spring.cache.type: caffeine` | La latencia casi no cambia, pero el catálogo deja de consultarse en cada request |

Para contar las consultas al catálogo (antes y después de k6):
```sql
SELECT seq_scan FROM pg_stat_user_tables WHERE relname = 'productos';
```

**Volver al estado roto:** `DROP INDEX idx_pedidos_codigo;` en la consola, `curl -X POST "localhost:8081/admin/retraso?ms=20"` y en `application.yml` `timeout-ms: 0` y `cache.type: none`.

## La cuenta del vídeo (ley de Little)
> Requests por segundo que aguanta tu pool ≈ conexiones ÷ tiempo que cada request retiene una conexión

Es una aproximación, pero en este laboratorio se cumple casi al milímetro: 200 hilos ÷ 2 s de espera = 100 req/s.

## Apagar
- Base de datos: escribe `salir` en su consola.
- Si cerraste la ventana sin `salir` y el puerto 5432 sigue ocupado, PostgreSQL quedó encendido. En Git Bash:
  ```bash
  "$(ls -d "$(cygpath -u "$LOCALAPPDATA")"/Temp/embedded-pg/PG-*/bin | head -1)/pg_ctl.exe" stop -m fast -D datos-postgres
  ```
- El resto: `Ctrl+C`.

---

Todo se debuggea.
