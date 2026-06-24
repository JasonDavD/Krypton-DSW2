# Guion de presentación — Krypton (monolito → microservicios)

> Relato y orden para presentar. Los comandos detallados de cada escenario están en
> `RESILIENCIA-PLAYBOOK.md`. Duración objetivo: ~10-15 min.

---

## 0. Antes de empezar (en casa, 5 min antes)
```bash
docker compose up -d --build      # levanta TODO, incluido el frontend
docker compose ps                 # esperá a que los 9 estén "Up" (db/mongo "healthy")
```
Abrí en el browser: **http://localhost:5173** (la app). Tené a mano una terminal para los `stop/start`.
Logueate una vez como **cliente** (registrate) y dejá otra pestaña con el **admin**
(`admin@krypton.pe` / `Admin123!`).

> Tip: hacé una compra de ensayo antes de presentar, así tenés un pedido para mostrar.

---

## 1. El pitch (30 seg)
"Krypton era un **monolito** de e-commerce (Spring Boot + React + MySQL). Lo migramos a
**microservicios** con un patrón **strangler-fig**: extrajimos los productos a `catalogo-service`,
las órdenes a `pedidos-service` (con su propia MongoDB), y las categorías a un microservicio
**SOAP** aparte. Un **API Gateway** es la única puerta de entrada, con **Eureka** para el
descubrimiento. Todo corre con `docker compose up`."

**Mostrá el diagrama / `docker compose ps`**: 9 contenedores, cada micro con su base.

---

## 2. La app funciona como siempre (happy path) — ~3 min
En el browser (cliente), recorré el flujo completo:
1. **Catálogo** → ver productos (vienen de `catalogo-service`).
2. **Agregar al carrito** → **Checkout** (boleta/DNI) → **Confirmar**.
3. **Pagar** (Yape o tarjeta) en `/pedidos/{id}`.
4. **Descargar el comprobante** (PDF) — aparece recién **después** de pagar.
5. (Admin) **Panel → Pedidos**: se ve el pedido recién creado.

**Qué decir:** "Para el usuario es una tienda normal. Por detrás, el checkout lo atiende
`pedidos-service`, que re-cotiza los precios llamando a `catalogo-service` y descuenta stock."

---

## 3. Es microservicios DE VERDAD: degradación elegante — ~5 min (el corazón)
"Si esto es un desacople real, apagar un micro **no debe tumbar todo**: el sistema degrada con
un mensaje claro y el resto sigue. Lo muestro."

> ⚠️ Apagá/prendé **de a uno** y esperá **~30-60s** (Eureka propaga). La 1ª llamada tras apagar
> puede tardar unos segundos antes del 503 — **eso es el circuit breaker**, señalalo.

1. **Apago catálogo** → `docker compose stop catalogo-service`
   - Browser: el catálogo muestra "No se pudieron cargar los productos"; el checkout, 503 controlado.
   - `docker compose start catalogo-service` (se recupera solo vía Eureka).
2. **Apago pedidos** → `docker compose stop pedidos-service`
   - "Mis pedidos" / detalle / admin de órdenes: **503 al instante** (antes se colgaba — circuit breaker).
   - `docker compose start pedidos-service`.
3. **Apago el SOAP de categorías** → `docker compose stop categorias-soap-service`
   - Las categorías dan 503; el resto sigue. Mostrá el **WSDL**: `http://localhost:8095/ws/categorias.wsdl`.
   - "Distinto protocolo (SOAP), base propia, micro aparte → desacople real."
   - `docker compose start categorias-soap-service`.

**Qué decir al cerrar el bloque:** "Cada microservicio es **dueño de su dato**; si se cae, el
sistema degrada con un mensaje claro y sigue andando en lo que no depende de él. Eso es el
desacople real — y se ve sin tocar una línea de código, solo apagando contenedores."

---

## 4. Detalles finos (si hay tiempo / preguntas)
- **Sincronización de stock**: comprar **descuenta** stock (pedidos → monolito → catálogo) y
  cancelar una orden lo **repone** (kardex ENTRADA/SALIDA consistente).
- **Resiliencia**: circuit breakers (Resilience4j) en monolito→pedidos y en el gateway; fallback
  best-effort (si un micro está caído, la operación crítica no se rompe).
- **Contract-first SOAP**: el `.xsd` es la fuente de verdad; JAXB genera las clases.

---

## 5. Cierre (30 seg)
"Resumen: partimos de un monolito y llegamos a un ecosistema de microservicios desacoplado,
resiliente y observable, con dos protocolos (REST y SOAP), orquestado con Docker Compose en un
solo comando. El monolito quedó como **puente** donde hacía falta, y cada servicio puede caer sin
arrastrar al resto."

---

## Apagar al terminar
```bash
docker compose down            # (agregá -v si querés borrar datos y re-sembrar limpio)
```
