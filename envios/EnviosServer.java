import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Servicio de envíos FICTICIO para el laboratorio.
 * Responde el estado de envío de un pedido con un retraso configurable.
 *
 *   java EnviosServer.java                      → arranca en el puerto 8081, retraso 20 ms
 *   GET  /envios/{codigo}                       → {"codigo":"PED-1","estado":"EN_RUTA"}
 *   POST /admin/retraso?ms=2000                 → a partir de ahora tarda 2 s
 *
 * Usa hilos virtuales para que ESTE servicio nunca sea el cuello de botella:
 * en la demo, lo lento es su respuesta, no su capacidad.
 */
public class EnviosServer {

    private static final AtomicLong retrasoMs = new AtomicLong(20);
    private static final String[] ESTADOS = {"PREPARANDO", "EN_RUTA", "ENTREGADO"};

    public static void main(String[] args) throws IOException {
        int puerto = args.length > 0 ? Integer.parseInt(args[0]) : 8081;
        HttpServer server = HttpServer.create(new InetSocketAddress(puerto), 1000);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        server.createContext("/envios/", EnviosServer::envio);
        server.createContext("/admin/retraso", EnviosServer::retraso);

        server.start();
        System.out.printf("Envíos escuchando en :%d · retraso %d ms%n", puerto, retrasoMs.get());
    }

    private static void envio(HttpExchange ex) throws IOException {
        try {
            Thread.sleep(retrasoMs.get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String codigo = ex.getRequestURI().getPath().substring("/envios/".length());
        String estado = ESTADOS[Math.floorMod(codigo.hashCode(), ESTADOS.length)];
        responder(ex, 200, "{\"codigo\":\"" + codigo + "\",\"estado\":\"" + estado + "\"}");
    }

    private static void retraso(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) {
            responder(ex, 200, "{\"retrasoMs\":" + retrasoMs.get() + "}");
            return;
        }
        String query = URI.create(ex.getRequestURI().toString()).getQuery();
        if (query == null || !query.startsWith("ms=")) {
            responder(ex, 400, "{\"error\":\"usa /admin/retraso?ms=2000\"}");
            return;
        }
        retrasoMs.set(Long.parseLong(query.substring(3)));
        System.out.printf("Nuevo retraso: %d ms%n", retrasoMs.get());
        responder(ex, 200, "{\"retrasoMs\":" + retrasoMs.get() + "}");
    }

    private static void responder(HttpExchange ex, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(status, body.length);
        try (var out = ex.getResponseBody()) {
            out.write(body);
        }
    }
}
