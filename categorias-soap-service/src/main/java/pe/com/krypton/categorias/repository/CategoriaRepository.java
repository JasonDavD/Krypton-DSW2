package pe.com.krypton.categorias.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import pe.com.krypton.categorias.ws.Categoria;

/**
 * Acceso a datos con JdbcTemplate (patrón SOAP-PLAYBOOK). La TRADUCCIÓN columnas↔contrato vive
 * acá: la tabla usa name/description y el contrato SOAP usa nombre/descripcion.
 */
@Repository
public class CategoriaRepository {

    private final JdbcTemplate template;

    public CategoriaRepository(JdbcTemplate template) {
        this.template = template;
    }

    public List<Categoria> listar() {
        return template.query("SELECT id, name, description FROM categories ORDER BY id",
                (rs, n) -> {
                    Categoria c = new Categoria();
                    c.setId(rs.getLong("id"));
                    c.setNombre(rs.getString("name"));
                    c.setDescripcion(rs.getString("description"));
                    return c;
                });
    }

    /** Inserta y devuelve el id generado. */
    public long registrar(Categoria c) {
        KeyHolder kh = new GeneratedKeyHolder();
        template.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO categories (name, description) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, c.getNombre());
            ps.setString(2, c.getDescripcion());
            return ps;
        }, kh);
        return kh.getKey() != null ? kh.getKey().longValue() : 0L;
    }

    /** Actualiza y devuelve las filas afectadas (0 = no existía). */
    public int actualizar(Categoria c) {
        return template.update("UPDATE categories SET name = ?, description = ? WHERE id = ?",
                c.getNombre(), c.getDescripcion(), c.getId());
    }

    /** Elimina y devuelve las filas afectadas (0 = no existía). */
    public int eliminar(long id) {
        return template.update("DELETE FROM categories WHERE id = ?", id);
    }
}
