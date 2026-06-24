# Krypton — E-commerce con arquitectura de microservicios

E-commerce B2C de tecnología (dispositivos y componentes). Proyecto académico **CIBERTEC —
Desarrollo de Servicios Web II (DSW2)**. Migrado de un monolito a una **arquitectura de
microservicios** con **servicios web REST y SOAP**, API Gateway, descubrimiento por Eureka y
resiliencia (circuit breakers). **Todo se levanta con un solo comando** vía Docker Compose.

## Arquitectura

```
                  React (frontend)
                        │
                  API Gateway (8094) ───── Eureka (8761 · descubrimiento)
     ┌──────────────────┼───────────────────────────┐
 catalogo-service   pedidos-service              backend (monolito) ──(SOAP)──► categorias-soap-service
  (REST · MySQL)     (REST · MongoDB)             (auth, carrito, admin)         (SOAP · MySQL)
```

- **catalogo-service** — productos (REST, MySQL).
- **pedidos-service** — órdenes y checkout (REST, MongoDB).
- **categorias-soap-service** — categorías (SOAP *contract-first*, MySQL).
- **backend (monolito)** — autenticación, carrito, panel admin y comprobantes; actúa de **puente REST→SOAP** hacia las categorías.
- **api-gateway** — única puerta de entrada (valida el JWT y propaga `X-User-Id`).
- **eureka-server** — descubrimiento de servicios.
- **frontend** — React + Vite (build estático servido por nginx).
- **db** (MySQL 8) y **mongo** (MongoDB 7) — persistencia, cada servicio con su propia base.

> **Resiliencia:** si un microservicio se cae, el sistema **degrada con elegancia** (responde un
> 503 controlado) en vez de colapsar. Ver [RESILIENCIA-PLAYBOOK.md](RESILIENCIA-PLAYBOOK.md).

## Requisitos

- **Docker Desktop** corriendo. *(Y nada más para levantar el proyecto — el frontend también va en Docker.)*
- Opcional, solo para desarrollo/tests locales: **JDK 17+** y **Node 22+**.

## Levantar TODO el proyecto (un comando)

Desde la raíz del repo:

```bash
docker compose up -d --build
```

Levanta los **9 servicios** (incluido el frontend). La primera vez compila las imágenes (tarda unos
minutos). Esperá **~60-90 s** a que arranquen y se registren en Eureka, y verificá:

```bash
docker compose ps        # los 9 en "Up"; db y mongo en "healthy"
```

Abrí la tienda en **http://localhost:5173**.

### Servicios y puertos

| Servicio | Puerto | Qué es |
|---|---|---|
| frontend | **5173** | la tienda (React + nginx) |
| api-gateway | 8094 | puerta de entrada (REST) |
| eureka-server | 8761 | panel de Eureka |
| backend (monolito) | 8080 | auth, carrito, admin, comprobantes |
| catalogo-service | 8092 | productos (REST) |
| pedidos-service | 8093 | órdenes (REST) |
| categorias-soap-service | 8095 | categorías (SOAP) · WSDL en `/ws/categorias.wsdl` |
| db (MySQL) | 3307 | bases `krypton` / `catalogo` / `categorias` |
| mongo (MongoDB) | 27017 | base `pedidos` |

### Credenciales

- **Admin:** `admin@krypton.pe` / `Admin123!`
- **Cliente:** registrate desde la tienda.

### Apagar

```bash
docker compose down            # agregá -v para borrar datos y re-sembrar desde cero
```

## Demo de microservicios (desacople / resiliencia)

El valor del desacople se demuestra apagando un servicio y viendo que el resto sigue:

```bash
docker compose stop pedidos-service          # "mis pedidos" / admin → 503 controlado; lo demás sigue
docker compose stop categorias-soap-service  # categorías → 503; resto sigue (servicio SOAP aparte)
docker compose start <servicio>              # se recupera solo (Eureka, ~30-60 s)
```

- Guía completa de escenarios + matriz de comportamiento: **[RESILIENCIA-PLAYBOOK.md](RESILIENCIA-PLAYBOOK.md)**.
- Guion para presentar: **[GUION-PRESENTACION.md](GUION-PRESENTACION.md)** y la carpeta **[presentacion/](presentacion/)**.

## Estructura del repo

```
Krypton-DSW2/
├── api-gateway/              # Spring Cloud Gateway (REST)
├── eureka-server/            # descubrimiento de servicios
├── catalogo-service/         # productos (REST · MySQL)
├── pedidos-service/          # órdenes (REST · MongoDB)
├── categorias-soap-service/  # categorías (SOAP · MySQL)
├── backend/                  # monolito (auth, carrito, admin, comprobantes)
├── frontend/                 # React + Vite (servido por nginx en Docker)
├── docker/mysql-init/        # init de las bases MySQL compartidas
├── docs/                     # documentación del proyecto
└── docker-compose.yml        # orquesta los 9 servicios
```

## Stack

| Capa | Tecnología |
|---|---|
| Microservicios | Spring Boot 3, Spring Cloud (Gateway, Eureka, OpenFeign, Resilience4j) |
| Servicio SOAP | Spring WS — *contract-first* (XSD → JAXB) |
| Frontend | React 19 + Vite + TypeScript (servido por nginx) |
| Persistencia | MySQL 8 (+ Flyway) · MongoDB 7 — una base por servicio |
| Seguridad | Spring Security + JWT (validado en el gateway) |
| Tests | JUnit 5, Mockito, Testcontainers |
| Orquestación | Docker Compose |

## Tests

Cada módulo Spring corre sus pruebas con Maven (el backend trae wrapper `./mvnw`):

```bash
cd backend && ./mvnw test          # en los micros: mvn test
```

Los tests de integración usan **Testcontainers** (levanta un MySQL real en Docker). La primera vez
en cada máquina, configurá Docker para Testcontainers (ver [docs/onboarding.md](docs/onboarding.md)).

## Documentación

| Doc | Contenido |
|---|---|
| [RESILIENCIA-PLAYBOOK.md](RESILIENCIA-PLAYBOOK.md) | Demo de apagar micros + matriz de comportamiento |
| [GUION-PRESENTACION.md](GUION-PRESENTACION.md) | Guion para la presentación |
| [docs/arquitectura-backend.md](docs/arquitectura-backend.md) | Arquitectura por capas del backend |
| [docs/modelo-datos.md](docs/modelo-datos.md) | Modelo de datos |
