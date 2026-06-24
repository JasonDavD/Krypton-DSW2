# Guion — Jason (apertura + arquitectura)

> Parte de la diapositiva: el título y la migración monolito → microservicios + Gateway/Eureka/Docker.
> Duración: ~40-50 s.

---

**Para decir:**

"Buenas. Somos el equipo de **Krypton**, una tienda de tecnología hecha con Spring Boot, React y
MySQL.

El punto de partida era un **monolito**: toda la aplicación en un solo bloque. Nuestro objetivo fue
migrarlo a una arquitectura de **microservicios**, y lo hicimos con el patrón **strangler-fig**: en
vez de reescribir todo de cero, fuimos **extrayendo funcionalidades** del monolito hacia servicios
independientes, sin romper la aplicación en el camino.

El resultado *(señalar el diagrama)*: un **API Gateway** que es la única puerta de entrada para el
frontend, **Eureka** para que los servicios se descubran entre sí, y cada microservicio con su
**propia base de datos**. Todo está orquestado con **Docker Compose**: con **un solo comando**
levantamos los 9 servicios más el frontend.

Le paso a **Joel**, que les va a contar los microservicios y el flujo del cliente."

---

**Cue / demo (opcional):** mostrar `docker compose ps` (9 contenedores "Up") y la app abierta en
`http://localhost:5173` funcionando como una tienda normal.
