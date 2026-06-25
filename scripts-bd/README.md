# Scripts de Bases de Datos — Krypton

Volcados (dumps) de las bases de datos del proyecto, una por microservicio. Reflejan el
estado real de las bases en ejecución (esquema + datos), no solo las migraciones Flyway.

## Bases de datos del sistema

| Archivo | Motor | Base | Servicio | Contenido |
|---|---|---|---|---|
| `01-krypton.sql`    | MySQL 8   | `krypton`    | monolito (backend)        | usuarios, productos, carritos, pedidos del monolito, movimientos de stock (kardex), imágenes |
| `02-catalogo.sql`   | MySQL 8   | `catalogo`   | catalogo-service          | productos del catálogo + galería de imágenes (réplica de lectura sincronizada) |
| `03-categorias.sql` | MySQL 8   | `categorias` | categorias-soap-service   | categorías (servicio SOAP) |
| `04-pedidos.orders.json`        | MongoDB 7 | `pedidos` | pedidos-service | pedidos y sus líneas |
| `04-pedidos.db_sequences.json`  | MongoDB 7 | `pedidos` | pedidos-service | secuencia del número de pedido (autoincremental) |

> El servicio de **pedidos** usa MongoDB (NoSQL): no tiene esquema SQL. Se incluye su
> export en JSON por colección (`orders` y `db_sequences`).

## Cómo restaurar

### MySQL (krypton, catalogo, categorias)
Cada archivo incluye `CREATE DATABASE` + `USE`, así que se restaura directo:

```bash
# contra el contenedor del stack (host port 3307)
mysql -h 127.0.0.1 -P 3307 -u root -proot < 01-krypton.sql

# o dentro del contenedor
docker compose exec -T db mysql -u root -proot < 01-krypton.sql
```

### MongoDB (pedidos)
```bash
# importar cada colección
docker compose exec -T mongo mongoimport --db pedidos --collection orders        --drop --jsonArray < 04-pedidos.orders.json
docker compose exec -T mongo mongoimport --db pedidos --collection db_sequences   --drop --jsonArray < 04-pedidos.db_sequences.json
```

## Fuente de verdad

El esquema lo crean las migraciones **Flyway** de cada servicio
(`*/src/main/resources/db/migration/`). Estos dumps son una foto del estado actual,
útil para entrega/inspección. Para levantar el sistema desde cero, usá Docker
(`docker compose up -d --build`) — Flyway crea y migra todo automáticamente.
