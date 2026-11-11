package lab.pedidos;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PedidoRepository {

    private final JdbcTemplate jdbc;

    public PedidoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Pedido> buscarPorCodigo(String codigo) {
        return jdbc.query("""
                        SELECT codigo, cliente, producto_id, cantidad, total, estado
                        FROM pedidos
                        WHERE codigo = ?
                        """,
                (rs, i) -> new Pedido(
                        rs.getString("codigo"),
                        rs.getString("cliente"),
                        rs.getInt("producto_id"),
                        rs.getInt("cantidad"),
                        rs.getBigDecimal("total"),
                        rs.getString("estado")),
                codigo).stream().findFirst();
    }

    public record Pedido(String codigo, String cliente, int productoId, int cantidad,
                         BigDecimal total, String estado) {
    }
}
