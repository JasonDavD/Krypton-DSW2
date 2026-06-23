# Playbook — Microservicios con Spring Cloud (Eureka + Gateway + Feign)

> Receta destilada del proyecto **MICRO** (farmacia). Pensada para reaplicarse a otros
> proyectos (ej. un Ecommerce). El stack técnico es el 20%; el diseño de **contextos
> delimitados** es el 80%. Leé la sección 0 antes de tocar una línea.

---

## 0. La regla de oro: NO partir por tablas

**Un microservicio = un contexto de negocio (bounded context), NO una tabla.**

Tentación a evitar: "11 tablas → 11 microservicios". Mal. Agrupá las tablas que
cambian juntas, por la misma razón de negocio y con el mismo dueño.

Ejemplo Ecommerce (~11 tablas → 3-4 servicios):
- **Catálogo**: producto, categoría, marca…
- **Pedidos**: orden, detalle_orden, carrito…
- **Clientes / Identidad**: usuario, dirección…
- **Pagos / Inventario** (según el caso)

Preguntas para definir un límite:
1. ¿Estas tablas cambian juntas?
2. ¿Tienen el mismo dueño de negocio?
3. ¿Puedo desplegar este servicio sin tocar los demás?

Cada servicio es **dueño de su base** (database-per-service). Nada de un micro
metiéndose en la tabla de otro: si lo necesita, lo pide por API (Feign).

---

## 1. Topología

```
                ┌─────────────┐
   Cliente ───► │   Gateway   │ (8094)  ── ruta lb://SERVICIO ─┐
                └─────────────┘                                 │
                       │ (se registra/consulta)                 ▼
                ┌─────────────┐                          ┌─────────────┐
                │   Eureka    │ (8761) ◄──registro────── │  Micro-A    │ (8092)
                │  (Discovery)│                          │  (BD propia)│
                └─────────────┘ ◄──registro──┐           └─────────────┘
                                             │              ▲ Feign (name)
                                      ┌─────────────┐       │
                                      │  Micro-B    │ (8093)│
                                      │  (BD propia)│───────┘
                                      └─────────────┘
```

**Orden de arranque (respetalo SIEMPRE):** Bases de datos → Eureka → micros → Gateway.

---

## 2. Stack y versiones (las que funcionaron)

- **Java 17**
- **Spring Boot 4.0.6** (unificá la MISMA versión en TODOS los módulos; mezclar 4.0.x y 4.1.x trae sorpresas)
- **Spring Cloud 2025.1.x** (gestionado por el BOM `spring-cloud-dependencies`)
- **OpenFeign** `spring-cloud-starter-openfeign`
- **Resilience4j** `spring-cloud-starter-circuitbreaker-resilience4j`
- **ModelMapper** 2.4.2 (entity ↔ DTO)
- **Lombok**

---

## 3. Eureka Server

```java
@EnableEurekaServer
@SpringBootApplication
public class EurekaServerApplication { ... }
```
```yaml
server:
  port: 8761
eureka:
  client:
    register-with-eureka: false   # el server no se registra a sí mismo
    fetch-registry: false
```

## 4. Gateway

```yaml
server:
  port: 8094
spring:
  application:
    name: API-GATEWAY
  cloud:
    gateway:
      routes:
        - id: CATALOGO
          uri: lb://CATALOGO        # lb:// = balanceo vía Eureka, NUNCA URL fija
          predicates:
            - Path=/catalogo/**
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka
```
Clase main del Gateway: `@EnableDiscoveryClient`.

## 5. Anatomía de un microservicio

```
com.empresa.servicio
├── ServicioApplication.java     (@SpringBootApplication @EnableDiscoveryClient [@EnableFeignClients])
├── entity/        → @Entity (JPA) o @Document (Mongo)
├── repository/    → extends JpaRepository / MongoRepository
├── dto/           → objetos de transporte (NO exponer la entity en la API)
├── interfaces/    → ICRUD<T,ID>
├── services/      → ICRUDImpl<T,ID> + servicios concretos
├── controller/    → @RestController, recibe/devuelve DTO
├── client/        → Feign clients hacia otros servicios
└── utils/         → ApiResponse, ValidationHandler, excepciones
```

### CRUD genérico (agnóstico de la tecnología)
Programá contra `ListCrudRepository`, el padre común de JPA y Mongo. Así el mismo
CRUD sirve para MySQL o MongoDB sin cambios.

```java
public abstract class ICRUDImpl<T, ID> implements ICRUD<T, ID> {
    public abstract ListCrudRepository<T, ID> repo();   // ← no JpaRepository

    public T buscarPorCodigo(ID cod) throws Exception {
        return repo().findById(cod)
            .orElseThrow(() -> new ModeloNotFoundException("Código " + cod + " no existe"));
    } // ← orElseThrow, NUNCA orElse(null)

    public void eliminar(ID cod) throws Exception {
        if (!repo().existsById(cod))
            throw new ModeloNotFoundException("Código " + cod + " no existe");
        repo().deleteById(cod);
    }
    // registrar/actualizar = repo().save(bean); listar = repo().findAll();
}
```

### Manejo de errores global
`@RestControllerAdvice` con handlers para validación (400), not-found (404),
negocio (409) y genérico (500). **El handler 500 NO devuelve `ex.getMessage()`**
(filtra SQL/tablas): logueá el detalle y devolvé un mensaje genérico.

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ApiResponse<?>> handle(Exception ex) {
    log.error("Error interno no controlado", ex);          // al log
    return new ResponseEntity<>(new ApiResponse<>(false, "Error interno del servidor", null),
                                HttpStatus.INTERNAL_SERVER_ERROR);  // al cliente, genérico
}
```

### Validación que sí valida
`@Valid` solo sirve si el objeto tiene restricciones. Poné `@NotBlank/@NotNull/@Min…`
en el DTO/entity que recibe el controller, o el `@Valid` no hace NADA.

---

## 6. Comunicación entre servicios (Feign)

**LA regla:** con Eureka, usás `name` (+ `path` si el endpoint destino tiene prefijo).
**NUNCA** `url` hardcodeada — eso bypassa el discovery y rompe en producción.

```java
@FeignClient(name = "CATALOGO", path = "/catalogo", fallback = CatalogoClientFallback.class)
public interface CatalogoClient {
    @GetMapping("/buscar/{id}")
    ProductoDTO buscar(@PathVariable Integer id);
}
```
Fallback (degradación elegante si el otro servicio cae):
```java
@Component
public class CatalogoClientFallback implements CatalogoClient {
    @Override public ProductoDTO buscar(Integer id) { return null; } // o un default seguro
}
```
Activación del circuit breaker:
```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```
```yaml
spring:
  cloud:
    openfeign:
      circuitbreaker:
        enabled: true
```
Clase main: `@EnableFeignClients`.

**Cuidado con el acoplamiento bidireccional:** si A llama a B y B llama a A, replanteá
si no deberían ser un solo servicio o si falta un evento/cola en el medio.

---

## 7. Seguridad / config mínima

- **Credenciales fuera del código:** `username: ${DB_USER:root}` / `password: ${DB_PASSWORD:mysql}`.
- Nunca commitear secretos en `application.yml`/`.properties`.

---

## 8. Persistencia políglota — pasar UN servicio a MongoDB

Mostrar que cada servicio elige su base es un gran ejercicio. Para migrar uno a Mongo
manteniendo el `id` entero (para no romper contratos con los otros servicios):

1. **pom:** sacá `spring-boot-starter-data-jpa` + driver SQL, poné `spring-boot-starter-data-mongodb`.
2. **Entity:** `@Entity/@Table/@Column` → `@Document(collection="...")`/`@Field`; `@Id` pasa a
   `org.springframework.data.annotation.Id`; **borrá `@GeneratedValue`** (Mongo no autoincrementa).
3. **Repository:** `JpaRepository` → `MongoRepository`.
4. **ID secuencial a mano** (reemplaza el AUTO_INCREMENT):
   ```java
   @Document(collection = "db_sequences")
   class DbSequence { @Id String id; int seq; }

   int next = mongoOperations.findAndModify(
       Query.query(Criteria.where("_id").is("productos_seq")),
       new Update().inc("seq", 1),
       FindAndModifyOptions.options().returnNew(true).upsert(true),
       DbSequence.class).getSeq();   // atómico
   ```
   En `registrar()`, si el código es null, asignás `next` antes de guardar.
5. **Migrar datos** MySQL → Mongo: `insertMany` con `_id: NumberInt(...)`, y **sembrar el
   contador con el código MÁXIMO existente** (no la cantidad de filas):
   ```js
   db.db_sequences.updateOne({_id:"productos_seq"}, {$set:{seq:NumberInt(MAX)}}, {upsert:true})
   ```
6. **Limpiá imports de `jakarta.persistence`** que queden sueltos: al sacar JPA esa
   librería desaparece y rompen la compilación.

---

## 9. ⚠️ GOTCHAS (la parte que cuesta sangre — leé esto)

1. **Spring Boot 4: la propiedad de Mongo es `spring.mongodb.uri`**, NO `spring.data.mongodb.uri`
   (esa era de Boot 3.x). Con la vieja, la app **no da error**, se conecta a la base
   default `test` y te volvés loco viendo listas vacías.
2. **Spring ignora EN SILENCIO las propiedades que no reconoce.** Una propiedad mal
   escrita es peor que un error: no falla, te deja con defaults que "parecen andar".
   Si algo lee de otra base/puerto del esperado, sospechá del nombre de la propiedad.
3. **Mongo crea la base/colección sola** al primer insert (lazy). Pero el **servidor**
   Mongo lo tenés que levantar vos. Y `show dbs` no muestra bases vacías.
4. **MongoDB no autoincrementa.** Si querés IDs enteros, los generás con la colección de
   secuencia (sección 8). Si migrás datos, sembrá el contador con el MÁX (ojo con huecos:
   si tenés 1..8 y 28, el contador va en 28, no en 8/9).
5. **Feign con `name` + `url` juntos** = ignora Eureka. Usá solo `name` (+ `path`).
6. **`findById().orElse(null)` + usar el objeto** = NullPointerException servido. `orElseThrow`.
7. **Orden de arranque:** Bases → Eureka → micros → Gateway. Si Eureka está caído, el
   Gateway no puede rutear (`lb://`) y los micros escupen "Connection refused: 8761".
8. **"Maven → Update Project" ≠ reconstruir.** Si cambiaste dependencias/config y seguís
   viendo comportamiento viejo, hacé un **clean** y volvé a correr. Verificá qué hay en
   `target/classes/application.yml` (eso es lo que realmente corre).
9. **Probá el micro DIRECTO** (`localhost:PUERTO/...`) para aislar problemas del Gateway/Eureka.

---

## 10. Checklist para el Ecommerce

- [ ] Mapear las ~11 tablas en 3-4 **bounded contexts** (sección 0). ESTO PRIMERO.
- [ ] Eureka Server arriba (8761).
- [ ] Un microservicio por contexto, cada uno con su base.
- [ ] Gateway con una ruta `lb://` por servicio.
- [ ] Feign solo donde un servicio NECESITA datos de otro (con fallback).
- [ ] Resilience4j + circuit breaker activado.
- [ ] DTOs en los bordes, `@RestControllerAdvice` global, validaciones reales.
- [ ] Credenciales por variables de entorno.
- [ ] (Opcional) Un servicio en Mongo para mostrar persistencia políglota.

---

## 11. Anti-patrones (NO hagas esto)

- ❌ Un microservicio por tabla.
- ❌ Un micro leyendo la base de datos de otro micro.
- ❌ `@FeignClient` con `url` fija.
- ❌ Feign sin fallback/circuit breaker.
- ❌ Devolver la Entity en la API (usá DTO).
- ❌ Secretos hardcodeados en el yml.
- ❌ Handler 500 devolviendo `ex.getMessage()` al cliente.
- ❌ Versiones de Spring Boot distintas entre módulos.
