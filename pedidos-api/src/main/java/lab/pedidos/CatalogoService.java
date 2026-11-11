package lab.pedidos;

import java.util.HashMap;
import java.util.Map;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CatalogoService {

    private final JdbcTemplate jdbc;

    public CatalogoService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Con spring.cache.type=none, @Cacheable no hace nada y se consulta la BD en cada request.
    // Con spring.cache.type=caffeine, se guarda en memoria (ver application.yml).
    @Cacheable("catalogo")
    public Map<Integer, String> productosActivos() {
        Map<Integer, String> productos = new HashMap<>();
        jdbc.query("SELECT id, nombre FROM productos WHERE activo",
                rs -> {
                    productos.put(rs.getInt("id"), rs.getString("nombre"));
                });
        return productos;
    }
}
