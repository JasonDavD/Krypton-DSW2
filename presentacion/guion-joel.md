# Guion — Joel (microservicios REST + funcionalidad)

> Parte de la diapositiva: catalogo-service, pedidos-service y la sincronización de datos.
> Duración: ~40-50 s.

---

**Para decir:**

"Gracias, Jason. Yo les explico los **microservicios** que extrajimos.

Separamos dos dominios principales, y cada uno tiene **su propia base de datos**:
- **`catalogo-service`**, que maneja los **productos** sobre **MySQL**, y
- **`pedidos-service`**, que maneja las **órdenes y el checkout** sobre **MongoDB**.

Están realmente **desacoplados**: no comparten base ni código.

Todo el **flujo del cliente** —ver el catálogo, agregar al carrito, pagar y descargar el
comprobante— entra por el **Gateway** y lo atienden estos microservicios. Por ejemplo, cuando un
cliente compra, `pedidos-service` **re-cotiza los precios** llamando a `catalogo-service` para que el
cliente no pueda alterarlos, y se **sincroniza el stock** entre los servicios. Y si una orden se
**cancela**, el stock se **repone** automáticamente.

Le dejo el cierre a **Alexander**, con la parte de SOAP y la resiliencia."

---

**Cue / demo (opcional):** hacer una compra en vivo en el frontend (carrito → checkout → pagar →
comprobante PDF). El comprobante aparece **recién después de pagar**.
