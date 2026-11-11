package lab.db;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * PostgreSQL REAL embebido para el laboratorio (sin Docker ni WSL).
 *
 *   java -jar db-embebida/target/db-embebida.jar
 *
 * - Arranca PostgreSQL en el puerto 5432 con los datos en ./datos-postgres
 * - La primera vez crea el usuario "lab", la base "pedidos" y 1 000 000 de pedidos ficticios
 * - Después abre una consola SQL ("pedidos=#") para usar en cámara, por ejemplo:
 *     EXPLAIN ANALYZE SELECT * FROM pedidos WHERE codigo = 'PED-500000';
 *     CREATE INDEX idx_pedidos_codigo ON pedidos (codigo);
 *     DROP INDEX idx_pedidos_codigo;
 *   Escribe "salir" para apagar la base de datos.
 */
public class BaseDeDatos {

    public static void main(String[] args) throws Exception {
        Path datos = Path.of(args.length > 0 ? args[0] : "datos-postgres").toAbsolutePath();
        boolean primeraVez = !Files.exists(datos.resolve("PG_VERSION"));

        System.out.println("Arrancando PostgreSQL en :5432 · datos en " + datos);
        EmbeddedPostgres pg = EmbeddedPostgres.builder()
                .setPort(5432)
                .setDataDirectory(datos)
                .setCleanDataDirectory(false)
                .setServerConfig("shared_buffers", "128MB")
                .setServerConfig("max_connections", "60")
                .setServerConfig("work_mem", "4MB")
                .start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                pg.close();
            } catch (Exception ignored) {
            }
        }));

        if (primeraVez) {
            crearUsuarioYBase();
            cargarDatos();
        }

        System.out.println("""

                ✓ PostgreSQL listo · jdbc:postgresql://localhost:5432/pedidos (lab / lab)
                Consola SQL: termina cada sentencia en una línea. "salir" para apagar.
                """);
        consola();
        System.exit(0);
    }

    private static void crearUsuarioYBase() throws SQLException {
        try (Connection c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", "postgres", "");
             Statement st = c.createStatement()) {
            st.execute("CREATE ROLE lab LOGIN PASSWORD 'lab'");
            st.execute("CREATE DATABASE pedidos OWNER lab");
        }
    }

    private static void cargarDatos() throws Exception {
        System.out.println("Primera vez: generando 1 000 000 de pedidos ficticios (1 o 2 minutos)...");
        Path sql = Path.of("db", "init", "01-esquema-y-datos.sql");
        String script = Files.readString(sql, StandardCharsets.UTF_8);
        long inicio = System.currentTimeMillis();
        try (Connection c = conectar(); Statement st = c.createStatement()) {
            st.execute(script);
        }
        System.out.printf("Datos cargados en %.1f s%n", (System.currentTimeMillis() - inicio) / 1000.0);
    }

    private static Connection conectar() throws SQLException {
        return DriverManager.getConnection("jdbc:postgresql://localhost:5432/pedidos", "lab", "lab");
    }

    private static void consola() throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        try (Connection c = conectar()) {
            while (true) {
                System.out.print("pedidos=# ");
                System.out.flush();
                String linea = in.readLine();
                if (linea == null) {
                    // Sin consola (por ejemplo, lanzado en segundo plano): mantener la BD encendida.
                    Thread.currentThread().join();
                }
                linea = linea.trim();
                if (linea.isEmpty()) continue;
                if (linea.equalsIgnoreCase("salir") || linea.equalsIgnoreCase("\\q")) return;
                ejecutar(c, linea.endsWith(";") ? linea.substring(0, linea.length() - 1) : linea);
            }
        }
    }

    private static void ejecutar(Connection c, String sql) {
        long inicio = System.nanoTime();
        try (Statement st = c.createStatement()) {
            boolean hayFilas = st.execute(sql);
            if (hayFilas) {
                imprimir(st.getResultSet());
            } else {
                System.out.println("OK (" + st.getUpdateCount() + ")");
            }
            System.out.printf("Tiempo: %.1f ms%n%n", (System.nanoTime() - inicio) / 1_000_000.0);
        } catch (SQLException e) {
            System.out.println("ERROR: " + e.getMessage() + "\n");
        }
    }

    private static void imprimir(ResultSet rs) throws SQLException {
        int columnas = rs.getMetaData().getColumnCount();
        int filas = 0;
        while (rs.next() && filas < 50) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= columnas; i++) {
                if (i > 1) sb.append(" | ");
                sb.append(rs.getString(i));
            }
            System.out.println(sb);
            filas++;
        }
    }
}
