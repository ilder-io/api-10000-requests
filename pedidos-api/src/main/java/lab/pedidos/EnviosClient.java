package lab.pedidos;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class EnviosClient {

    private final RestClient rest;

    public EnviosClient(@Value("${lab.envios.url}") String url,
                        @Value("${lab.envios.timeout-ms}") long timeoutMs) {

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        if (timeoutMs > 0) {
            // Con timeout: si envíos no responde a tiempo, dejamos de esperar.
            factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        }
        // Con timeout-ms = 0 no configuramos nada: el hilo espera lo que tarde envíos.

        this.rest = RestClient.builder()
                .baseUrl(url)
                .requestFactory(factory)
                .build();
    }

    public String estadoDe(String codigo) {
        try {
            EstadoEnvio estado = rest.get()
                    .uri("/envios/{codigo}", codigo)
                    .retrieve()
                    .body(EstadoEnvio.class);
            return estado != null ? estado.estado() : "NO_DISPONIBLE";
        } catch (RestClientException e) {
            // Respuesta de reserva: el pedido se devuelve igual, sin el estado del envío.
            return "NO_DISPONIBLE";
        }
    }

    public record EstadoEnvio(String codigo, String estado) {
    }
}
