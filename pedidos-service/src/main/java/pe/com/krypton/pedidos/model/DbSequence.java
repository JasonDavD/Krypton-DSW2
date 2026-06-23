package pe.com.krypton.pedidos.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Contador atómico para emular el autoincrement que Mongo no tiene. Cada documento
 * representa una secuencia (p.ej. {@code _id = "orders_seq"}). SequenceGenerator hace
 * findAndModify($inc seq, returnNew, upsert) para obtener el siguiente id entero.
 */
@Document("db_sequences")
public class DbSequence {

    @Id
    private String id;
    private long seq;

    public DbSequence() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getSeq() {
        return seq;
    }

    public void setSeq(long seq) {
        this.seq = seq;
    }
}
