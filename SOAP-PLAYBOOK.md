# SOAP Playbook — Web Service contract-first con Spring Boot

> Patrón reutilizable para montar un web service SOAP (CRUD) con Spring Boot + JAXB + JdbcTemplate.
> Dejá este archivo en la raíz del proyecto nuevo. Si lo renombrás a `CLAUDE.md`, Claude Code lo carga
> solo como instrucciones. El ejemplo usa la entidad **Producto**; al final está cómo adaptarlo a cualquier entidad.

---

## 1. Principio que manda: CONTRACT-FIRST

El **XSD es la fuente de verdad**. NO se escriben clases de modelo a mano: las genera Maven a partir del XSD
(carpeta `target/generated-sources/jaxb`). Flujo de una petición:

```
Cliente (XML SOAP)
  -> MessageDispatcherServlet  (portero, ruta /ws/*)
  -> Endpoint                  (capa web: rutea por nombre del mensaje)
  -> Service                   (capa negocio: arma el objeto Response)
  -> Repository                (capa datos: SQL con JdbcTemplate)
  -> MySQL
```

Regla de oro: **cada vez que tocás el `.xsd`, corré `mvn clean compile`** para regenerar las clases.

---

## 2. Checklist de implementación (en orden)

1. [ ] Base de datos: crear DB + tabla + datos de prueba (`db/schema.sql`)
2. [ ] `pom.xml`: dependencias + plugin JAXB
3. [ ] `src/main/resources/wsdl/<entidad>.xsd`: el contrato
4. [ ] `mvn clean compile` → genera las clases JAXB
5. [ ] `src/main/resources/application.properties`: puerto + datasource
6. [ ] Clases Java (de abajo hacia arriba): Repository → Service → Endpoint → Config → Application
7. [ ] `mvn spring-boot:run` y probar con el WSDL + requests SOAP

---

## 3. Estructura de carpetas (paquete = carpeta)

```
proyecto/
├── pom.xml                              (raíz)
├── db/schema.sql                        (raíz/db)
└── src/main/
    ├── java/com/example/<app>/
    │   ├── <App>Application.java         (paquete base)
    │   ├── config/WebServiceConfig.java  (...config)
    │   ├── endpoint/ProductoEndpoint.java(...endpoint)
    │   ├── services/ProductoService.java (...services)
    │   └── repository/ProductoRepository.java (...repository)
    └── resources/
        ├── application.properties
        └── wsdl/producto.xsd
```

Spring Boot solo escanea componentes dentro del paquete de la clase `@SpringBootApplication`.

---

## 4. Plantillas de archivos

### 4.1 `db/schema.sql`

```sql
CREATE DATABASE IF NOT EXISTS soap_2026 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE soap_2026;
CREATE TABLE IF NOT EXISTS tb_producto (
  id_pro    INT          NOT NULL AUTO_INCREMENT,
  nom_pro   VARCHAR(100) NOT NULL,
  stock_pro INT          NOT NULL DEFAULT 0,
  pre_pro   DOUBLE       NOT NULL DEFAULT 0,
  PRIMARY KEY (id_pro)
);
INSERT INTO tb_producto (nom_pro, stock_pro, pre_pro) VALUES ('Demo', 10, 1.5);
```

### 4.2 `pom.xml` — partes clave

```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>3.5.7</version>
</parent>
<properties><java.version>17</java.version></properties>

<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web-services</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jdbc</artifactId>
  </dependency>
  <dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>9.0.0</version>
  </dependency>
  <dependency>
    <groupId>jakarta.xml.bind</groupId>
    <artifactId>jakarta.xml.bind-api</artifactId>
  </dependency>
  <dependency>
    <groupId>org.glassfish.jaxb</groupId>
    <artifactId>jaxb-runtime</artifactId>
  </dependency>
  <dependency>
    <groupId>wsdl4j</groupId>
    <artifactId>wsdl4j</artifactId>
    <version>1.6.3</version>
  </dependency>
</dependencies>

<build><plugins>
  <plugin>
    <groupId>org.jvnet.jaxb</groupId>
    <artifactId>jaxb-maven-plugin</artifactId>
    <version>4.0.8</version>
    <executions><execution>
      <id>generate-jaxb</id>
      <goals><goal>generate</goal></goals>
      <configuration>
        <schemaDirectory>${project.basedir}/src/main/resources/wsdl</schemaDirectory>
        <schemaIncludes><include>**/*.xsd</include></schemaIncludes>
        <generateDirectory>${project.build.directory}/generated-sources/jaxb</generateDirectory>
      </configuration>
    </execution></executions>
  </plugin>
</plugins></build>
```

### 4.3 `src/main/resources/wsdl/producto.xsd` — el contrato

```xml
<?xml version="1.0" encoding="UTF-8"?>
<xsd:schema xmlns:xsd="http://www.w3.org/2001/XMLSchema"
    targetNamespace="http://example.com/soapclinica"
    xmlns:tns="http://example.com/soapclinica"
    elementFormDefault="qualified">

  <xsd:complexType name="producto">
    <xsd:sequence>
      <xsd:element name="codigo" type="xsd:int"/>
      <xsd:element name="nombre" type="xsd:string"/>
      <xsd:element name="stock"  type="xsd:int"/>
      <xsd:element name="precio" type="xsd:double"/>
    </xsd:sequence>
  </xsd:complexType>

  <xsd:element name="getProductosRequest"><xsd:complexType/></xsd:element>
  <xsd:element name="getProductosResponse"><xsd:complexType><xsd:sequence>
    <xsd:element name="lista" type="tns:producto" maxOccurs="unbounded" minOccurs="0"/>
  </xsd:sequence></xsd:complexType></xsd:element>

  <xsd:element name="postProductoRequest"><xsd:complexType><xsd:sequence>
    <xsd:element name="producto" type="tns:producto"/>
  </xsd:sequence></xsd:complexType></xsd:element>
  <xsd:element name="postProductoResponse"><xsd:complexType><xsd:sequence>
    <xsd:element name="salida" type="xsd:int"/>
  </xsd:sequence></xsd:complexType></xsd:element>

  <xsd:element name="putProductoRequest"><xsd:complexType><xsd:sequence>
    <xsd:element name="producto" type="tns:producto"/>
  </xsd:sequence></xsd:complexType></xsd:element>
  <xsd:element name="putProductoResponse"><xsd:complexType><xsd:sequence>
    <xsd:element name="salida" type="xsd:int"/>
  </xsd:sequence></xsd:complexType></xsd:element>

  <xsd:element name="deleteProductoRequest"><xsd:complexType><xsd:sequence>
    <xsd:element name="codigo" type="xsd:int"/>
  </xsd:sequence></xsd:complexType></xsd:element>
  <xsd:element name="deleteProductoResponse"><xsd:complexType><xsd:sequence>
    <xsd:element name="salida" type="xsd:int"/>
  </xsd:sequence></xsd:complexType></xsd:element>
</xsd:schema>
```

### 4.4 `application.properties`

```properties
server.port=8098
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.url=jdbc:mysql://localhost:3306/soap_2026?serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=mysql
```

### 4.5 `WebServiceConfig.java`

```java
@EnableWs
@Configuration
public class WebServiceConfig extends WsConfigurerAdapter {

  @Bean
  public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext ctx) {
    MessageDispatcherServlet servlet = new MessageDispatcherServlet();
    servlet.setApplicationContext(ctx);
    servlet.setTransformWsdlLocations(true);
    return new ServletRegistrationBean<>(servlet, "/ws/*");
  }

  @Bean(name = "producto")   // <- el nombre define la URL: /ws/producto.wsdl
  public DefaultWsdl11Definition defaultWsdl11Definition(XsdSchema productoSchema) {
    DefaultWsdl11Definition def = new DefaultWsdl11Definition();
    def.setPortTypeName("ProductoPort");
    def.setTargetNamespace("http://example.com/soapclinica");
    def.setLocationUri("/ws");
    def.setSchema(productoSchema);
    return def;
  }

  @Bean
  public XsdSchema productoSchema() {
    return new SimpleXsdSchema(new ClassPathResource("wsdl/producto.xsd"));
  }
}
```

### 4.6 `ProductoRepository.java`

```java
@Repository
public class ProductoRepository {
  @Autowired JdbcTemplate template;

  public List<Producto> listProductos() {
    return template.query("select * from tb_producto",
      (rs, n) -> new Producto(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getDouble(4)));
  }
  public int registrarProductos(Producto b) {
    return template.update("insert into tb_producto values(null,?,?,?)",
      b.getNombre(), b.getStock(), b.getPrecio());
  }
  public int actualizarProducto(Producto b) {
    return template.update("update tb_producto set nom_pro=?,stock_pro=?,pre_pro=? where id_pro=?",
      b.getNombre(), b.getStock(), b.getPrecio(), b.getCodigo());
  }
  public int eliminarProducto(int codigo) {
    return template.update("delete from tb_producto where id_pro=?", codigo);
  }
}
```

### 4.7 `ProductoService.java`

```java
@Service
public class ProductoService {
  @Autowired private ProductoRepository repo;

  public GetProductosResponse findAll() {
    GetProductosResponse r = new GetProductosResponse();
    r.setLista(repo.listProductos());
    return r;
  }
  public PostProductoResponse save(Producto b) {
    PostProductoResponse r = new PostProductoResponse();
    r.setSalida(repo.registrarProductos(b));
    return r;
  }
  public PutProductoResponse update(Producto b) {
    PutProductoResponse r = new PutProductoResponse();
    r.setSalida(repo.actualizarProducto(b));
    return r;
  }
  public DeleteProductoResponse delete(int cod) {
    DeleteProductoResponse r = new DeleteProductoResponse();
    r.setSalida(repo.eliminarProducto(cod));
    return r;
  }
}
```

### 4.8 `ProductoEndpoint.java`

```java
@Endpoint
public class ProductoEndpoint {
  @Autowired private ProductoService servicio;
  private static final String NS = "http://example.com/soapclinica";

  @PayloadRoot(namespace = NS, localPart = "getProductosRequest")
  @ResponsePayload
  public GetProductosResponse listarTodos() { return servicio.findAll(); }

  @PayloadRoot(namespace = NS, localPart = "postProductoRequest")
  @ResponsePayload
  public PostProductoResponse nuevo(@RequestPayload PostProductoRequest req) {
    return servicio.save(req.getProducto());
  }
  @PayloadRoot(namespace = NS, localPart = "putProductoRequest")
  @ResponsePayload
  public PutProductoResponse modificar(@RequestPayload PutProductoRequest req) {
    return servicio.update(req.getProducto());
  }
  @PayloadRoot(namespace = NS, localPart = "deleteProductoRequest")
  @ResponsePayload
  public DeleteProductoResponse eliminar(@RequestPayload DeleteProductoRequest req) {
    return servicio.delete(req.getCodigo());
  }
}
```

---

## 5. Probar

```bash
mvn clean compile
mvn spring-boot:run
```

- WSDL: `http://localhost:8098/ws/producto.wsdl`
- Endpoint SOAP (POST, Content-Type text/xml): `http://localhost:8098/ws`
- Listar:

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:soap="http://example.com/soapclinica">
  <soapenv:Body><soap:getProductosRequest/></soapenv:Body>
</soapenv:Envelope>
```

Atajo: en SoapUI, pegá la URL del WSDL y te arma los 4 requests solo.

---

## 6. Gotchas (los errores que SIEMPRE aparecen)

| Síntoma | Causa | Fix |
|---|---|---|
| No encuentra `Producto`/`*Response` | No generaste las clases | `mvn clean compile` |
| El listado corta en N ítems | `maxOccurs` fijo en el XSD | usar `maxOccurs="unbounded"` |
| `Unknown column 'x'` | nombre de columna ≠ base | alinear SQL del repository con la tabla |
| Cambié el XSD y no se refleja | clases viejas en `target` | `mvn clean compile` de nuevo |
| WSDL en URL inesperada | nombre del `@Bean` en config | el nombre del bean = nombre del `.wsdl` |
| Las columnas de la base no son las del contrato | es esperado | la traducción vive en el RowMapper del repository |

---

## 7. Cómo adaptarlo a OTRA entidad (ej: Cliente)

Buscar y reemplazar de forma consistente:

1. **XSD**: `producto`/`Producto` → `cliente`/`Cliente`; redefinir los campos del `complexType`.
2. **Tabla y SQL**: `tb_producto` y columnas → las de la entidad nueva (mantené el RowMapper alineado por posición de columna).
3. **Clases**: renombrar `ProductoRepository/Service/Endpoint` y los tipos generados (`GetClientesResponse`, etc.).
4. **Config**: `@Bean(name = "cliente")` → WSDL en `/ws/cliente.wsdl`; `portTypeName` → `ClientePort`.
5. **`localPart`** de cada `@PayloadRoot` debe coincidir EXACTO con el nombre del elemento del XSD.
6. Regenerar: `mvn clean compile`.

El patrón de 3 capas (Endpoint recibe → Service arma → Repository persiste) no cambia nunca. Lo único que cambia es la entidad.
