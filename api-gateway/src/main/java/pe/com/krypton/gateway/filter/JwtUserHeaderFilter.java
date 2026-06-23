package pe.com.krypton.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Handoff de identidad hacia pedidos-service.
 *
 * pedidos NO valida JWT (scope enfocado): confia en el header X-User-Id que le
 * inyecta ESTE gateway. Por eso, para las rutas /api/orders/**, el gateway:
 *   1. exige un Bearer token valido (mismo secreto que el monolito),
 *   2. extrae el claim numerico `userId`,
 *   3. lo inyecta como X-User-Id (sobrescribiendo cualquiera que mande el cliente,
 *      asi no se puede spoofear).
 *
 * El resto de rutas pasa sin tocar: catalogo es publico y el monolito valida su
 * propio JWT.
 */
@Component
public class JwtUserHeaderFilter implements GlobalFilter, Ordered {

    private static final String ORDERS_PREFIX = "/api/orders";
    private static final String USER_HEADER = "X-User-Id";

    private final SecretKey key;

    public JwtUserHeaderFilter(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Preflight CORS: no lleva token y lo resuelve globalcors. Nunca validar JWT acá
        // (defensa extra; con add-to-simple-url-handler-mapping no debería ni llegar).
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        String path = request.getURI().getPath();

        // Solo pedidos necesita el handoff. El resto sigue de largo.
        if (!path.startsWith(ORDERS_PREFIX)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(authHeader.substring(7))
                    .getPayload();

            Object userId = claims.get("userId");
            if (userId == null) {
                return unauthorized(exchange);
            }

            // set() sobrescribe: un X-User-Id que venga del cliente se descarta.
            ServerHttpRequest mutated = request.mutate()
                    .headers(h -> h.set(USER_HEADER, String.valueOf(userId)))
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());

        } catch (JwtException | IllegalArgumentException e) {
            return unauthorized(exchange); // firma invalida, expirado o malformado
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1; // antes del filtro de ruteo
    }
}
