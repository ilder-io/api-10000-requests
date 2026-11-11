package lab.pedidos;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PedidoController {

    private final PedidoRepository pedidos;
    private final CatalogoService catalogo;
    private final EnviosClient envios;

    public PedidoController(PedidoRepository pedidos, CatalogoService catalogo, EnviosClient envios) {
        this.pedidos = pedidos;
        this.catalogo = catalogo;
        this.envios = envios;
    }

    @GetMapping("/pedidos/{codigo}")
    public ResponseEntity<PedidoResponse> obtener(@PathVariable String codigo) {
        // 1. El pedido, buscado por su código (¿hay índice en "codigo"?)
        var pedido = pedidos.buscarPorCodigo(codigo);
        if (pedido.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var p = pedido.get();

        // 2. El catálogo de productos (cambia una vez al día, ¿hace falta pedirlo siempre?)
        Map<Integer, String> productos = catalogo.productosActivos();

        // 3. El estado del envío, en otro servicio (¿y si tarda?)
        String envio = envios.estadoDe(codigo);

        return ResponseEntity.ok(new PedidoResponse(
                p.codigo(),
                p.cliente(),
                productos.getOrDefault(p.productoId(), "desconocido"),
                p.cantidad(),
                p.total(),
                p.estado(),
                envio));
    }

    // No toca la base de datos ni el servicio de envíos.
    // Sirve para ver si la API entera sigue respondiendo.
    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    public record PedidoResponse(String codigo, String cliente, String producto, int cantidad,
                                 BigDecimal total, String estado, String envio) {
    }
}
