package pe.com.krypton.pedidos.service;

import static org.springframework.data.mongodb.core.FindAndModifyOptions.options;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import pe.com.krypton.pedidos.model.DbSequence;

/**
 * Emula el autoincrement que Mongo no tiene. {@code findAndModify} con $inc es ATÓMICO
 * a nivel de documento: dos checkouts concurrentes nunca obtienen el mismo número.
 * {@code returnNew(true)} devuelve el valor ya incrementado; {@code upsert(true)} crea
 * la secuencia la primera vez (arranca en 1).
 */
@Service
public class SequenceGenerator {

    private final MongoOperations mongoOperations;

    public SequenceGenerator(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    public long nextId(String seqName) {
        DbSequence counter = mongoOperations.findAndModify(
                query(where("_id").is(seqName)),
                new Update().inc("seq", 1),
                options().returnNew(true).upsert(true),
                DbSequence.class);
        // counter nunca es null con upsert(true)+returnNew(true), pero blindamos por si acaso.
        return counter != null ? counter.getSeq() : 1L;
    }
}
