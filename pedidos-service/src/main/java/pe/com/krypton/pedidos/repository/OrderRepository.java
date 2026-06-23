package pe.com.krypton.pedidos.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import pe.com.krypton.pedidos.model.Order;

/**
 * Repositorio de pedidos sobre MongoDB. El id es {@code Long} (generado por
 * SequenceGenerator). Las queries derivan del nombre del método (Spring Data).
 */
public interface OrderRepository extends MongoRepository<Order, Long> {

    /** Pedidos del usuario ordenados por fecha DESC (más recientes primero). */
    List<Order> findByUserIdOrderByOrderDateDesc(Long userId);

    /** Pedido del usuario por id — vacío si no existe o no es del usuario (anti-IDOR). */
    Optional<Order> findByIdAndUserId(Long id, Long userId);
}
