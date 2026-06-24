# Guion — Alexander (SOAP + resiliencia)

> Parte de la diapositiva: categorias-soap-service y la degradación elegante.
> Duración: ~40-50 s. Es el cierre fuerte de la presentación.

---

**Para decir:**

"Gracias, Joel. Yo cierro con dos puntos.

**Primero, SOAP.** Además de REST, implementamos un microservicio **SOAP** para las **categorías**
—`categorias-soap-service`— con **su propia base de datos** y un enfoque **contract-first**: el
contrato XSD es la fuente de verdad y de ahí se generan las clases. Con esto mostramos que el
sistema integra **distintos protocolos** (REST y SOAP) de forma desacoplada.

**Y lo más importante, la resiliencia.** Como el desacople es real, si un microservicio se cae el
sistema **no colapsa**: gracias a los **circuit breakers** y **fallbacks**, **degrada con
elegancia** —responde un mensaje claro— y el resto de la aplicación **sigue funcionando**.

Y eso se ve **sin tocar una sola línea de código**: solo apagando un contenedor. *(demo)* Acá apago
[el servicio] y, mientras el resto sigue andando, esa parte muestra un *'servicio no disponible'*
controlado en vez de romperse.

En resumen: partimos de un monolito y llegamos a un ecosistema de microservicios **desacoplado,
resiliente y con dos protocolos**, levantado con un solo comando. **Gracias.**"

---

**Cue / demo (opcional):** `docker compose stop pedidos-service` (o el que prefieran) → mostrar el
**503 controlado** en la app, y que el catálogo/login siguen funcionando. Recordar: esperar ~30-60 s
y prender/apagar de a uno (Eureka). Detalle de comandos en `RESILIENCIA-PLAYBOOK.md`.
