# Playbook — Demo de Resiliencia (microservicios Krypton)

Esta guía demuestra que el desacople en microservicios es **real**: cada servicio es dueño de su
dato y, si uno se cae, el sistema **degrada con elegancia** (mensaje claro, sin colgarse) en vez de
romperse. Pensada para correr frente al profe, **100% por el browser** — los únicos comandos de
terminal son los `docker compose stop/start` que simulan la caída.

**Arquitectura:** `frontend (Vite)` → `api-gateway :8094` → `catalogo-service` (productos),
`pedidos-service` (órdenes), `backend` (monolito: auth, carrito, admin, comprobante).
Aparte, `categorias-soap-service :8095` expone las categorías por **SOAP** (no va por el gateway:
el monolito lo consume por URL fija como puente REST→SOAP). Descubrimiento por Eureka; resiliencia
con circuit breakers (Resilience4j) en monolito→pedidos y en el gateway, y fallback SOAP en monolito→categorías.

---

## 0. Preparación (desde cero)

**Requisitos:** Docker Desktop corriendo y Node instalado.

### 1) Levantar todo el stack
```bash
cd "F:/CIBERTEC/VI/EFSRT V/Proyecto/Krypton-DSW2"
docker compose up -d --build      # 1ra vez o tras cambios de código
# Si las imágenes ya están construidas, alcanza con:  docker compose up -d
```
El orden de arranque (bases → Eureka → micros → gateway) lo resuelven los `depends_on` + healthchecks.

### 2) Esperar el arranque y verificar
Las apps Spring tardan **~60-90s** en bootear y registrarse en Eureka. Verificá el estado:
```bash
docker compose ps                 # los 7 servicios "Up"; db y mongo "healthy"
```
Confirmá que el gateway ya rutea (repetí hasta obtener `200`):
```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8094/api/products    # -> 200
```

### 3) El frontend YA está en Docker
No hace falta `npm run dev`: el `frontend` es parte del `docker compose up` (build estático
de Vite servido por nginx, con la URL del gateway horneada). Abrí **http://localhost:5173**.

### 4) Tener dos sesiones listas
Abrí dos sesiones (dos ventanas, o una normal + una incógnito):
- **Cliente**: registrate desde el frontend.
- **Admin**: `admin@krypton.pe` / `Admin123!`.

### 5) Ensayo del happy path (antes de romper nada)
Como **cliente**: catálogo → agregar al carrito → checkout → pagar en `/pedidos/{id}`.
Así tenés un pedido real para mostrar en los escenarios.

---

## Escenario 1 — Apago el catálogo
```bash
docker compose stop catalogo-service
```
En el browser (cliente):
- **Catálogo** (recargá): aparece **"No se pudieron cargar los productos."** — no pantalla en blanco.
- **Comprar** (con algo en el carrito): el checkout muestra **"El catálogo no está disponible…"**.

Recuperar:
```bash
docker compose start catalogo-service     # esperá ~30-60s y recargá → vuelve
```

---

## Escenario 2 — Apago los pedidos (el caso fuerte)
```bash
docker compose stop pedidos-service
```
En el browser (cliente):
- **Mis pedidos**: dice **"No pudimos cargar tus pedidos. El servicio… no está disponible."** ✅ (antes decía engañosamente "no tenés pedidos").
- **Abrí un pedido** `/pedidos/{id}`: **"No pudimos cargar el pedido. Servicio no disponible."** ✅ (antes "no existe").
- **Checkout**: **"El servicio de pedidos no está disponible…"**.

En el browser (admin):
- **Panel → Pedidos**: **"No se pudieron cargar los pedidos."** al instante (antes se **colgaba**).

Recuperar:
```bash
docker compose start pedidos-service       # ~30-60s
```

---

## Escenario 3 — Apago el backend (monolito)
```bash
docker compose stop backend
```
En el browser:
- **Catálogo**: sigue cargando normal (es micro independiente).
- **Comprar**: la compra **se completa** y te lleva a `/pedidos/{id}`. ⚠️ El stock de esa venta **no se descuenta** (best-effort) y **no se reconcilia** al volver — es el trade-off consciente.
- **Login / registro / panel admin**: no andan (la auth vive en el backend; es inherente, no es degradable).

Recuperar:
```bash
docker compose start backend               # ~30-60s; el admin tarda un poco más (Eureka re-descubre pedidos)
```

---

## Escenario 4 — Apago el micro **SOAP de categorías** (desacople SOAP)
```bash
docker compose stop categorias-soap-service
```
En el browser:
- **Catálogo** (sidebar de categorías) y **admin → Categorías**: muestran **"servicio no disponible"** (el monolito recibe un **503** del puente REST→SOAP). El resto (productos, pedidos, login) sigue.
- Terminal (lo contundente — se ve el SOAP desacoplado):
  ```bash
  curl -i http://localhost:8094/api/categories
  # HTTP/1.1 503  →  {"status":503,"error":"El servicio de categorías no está disponible..."}
  ```

**Qué decir:** "Las categorías se extrajeron a un microservicio **SOAP** con su propia base. El monolito ya no es dueño: actúa de puente REST→SOAP. Si el micro SOAP cae, **falla rápido con 503** y el resto del sistema sigue — desacople real, distinto protocolo (SOAP) y todo."

> Para ver el contrato SOAP directo: `http://localhost:8095/ws/categorias.wsdl` (POST SOAP a `:8095/ws`).

Recuperar:
```bash
docker compose start categorias-soap-service     # ~20-40s y vuelve
```

---

## Tips para la demo
- **Apagá/prendé de a uno** y esperá ~30-60s antes de probar (Eureka tarda en propagar).
- La **1ª acción tras la caída** puede tardar unos segundos (el circuito todavía no abrió); las siguientes son instantáneas — ese es el **circuit breaker** en acción, buen punto para señalar.
- (Opcional) `docker compose logs -f api-gateway` mientras apagás un micro: se ve el circuito abriendo.
- **Frase de cierre:** *"Cada microservicio es dueño de su dato; si se cae, el sistema degrada con un mensaje claro y sigue andando en lo que no depende de él — eso es el desacople real."*

---

## Apéndice — Matriz de comportamiento

| Flujo | sin catálogo | sin pedidos | sin backend | sin SOAP categorías |
|---|---|---|---|---|
| Login | OK | OK | ❌ cae (auth) | OK |
| Ver catálogo | 🟢 503 controlado | OK | OK | OK (productos; nombres de categoría degradan a null) |
| Categorías (listar / admin) | OK | OK | ❌ cae (backend) | 🟢 503 controlado |
| Mis pedidos | OK | 🟢 503 controlado | OK | OK |
| Checkout | 🟢 503 controlado | 🟢 503 controlado | 🟢 201 (best-effort: no descuenta stock) | OK |
| Admin órdenes/reportes | OK | 🟢 503 rápido (~30ms) | ❌ cae (backend) | OK |

🟢 = degrada con elegancia · ❌ cae = el servicio caído **es** el que provee esa función (no degradable).
