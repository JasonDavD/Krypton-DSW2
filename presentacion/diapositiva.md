# KRYPTON — De monolito a microservicios
*E-commerce de tecnología · Spring Boot · React · Docker*

---

## ¿Qué implementamos?

- **Migración** de un **monolito** a una arquitectura de **microservicios** (patrón *strangler-fig*: se extraen funcionalidades sin romper la app).

- **Servicios desacoplados, cada uno con su propia base de datos:**
  - `catalogo-service` → productos *(MySQL)*
  - `pedidos-service` → órdenes y checkout *(MongoDB)*
  - `categorias-soap-service` → categorías *(SOAP, base propia)*
  - `monolito` → autenticación, carrito y panel admin

- **API Gateway** como única puerta de entrada + **Eureka** para el descubrimiento de servicios.

- **Resiliencia (degradación elegante):** circuit breakers + fallbacks → si un servicio se cae, el resto **sigue funcionando** con un mensaje claro.

- **Sincronización de datos** entre servicios (stock y catálogo).

- **Orquestación con Docker Compose:** un solo comando levanta los **9 servicios + el frontend**.

---

```
                React (frontend)
                       │
                 API Gateway  ──────  Eureka (descubrimiento)
        ┌──────────────┼──────────────┐
   catalogo-service  pedidos-service   monolito ──(REST/SOAP)──► categorias-soap
     (MySQL)           (MongoDB)        (auth, carrito, admin)      (SOAP · MySQL)
```

> **Stack:** Java 17/21 · Spring Boot · Spring Cloud (Gateway, Eureka, OpenFeign, Resilience4j) · Spring WS (SOAP) · React + Vite · MySQL · MongoDB · Docker Compose.
