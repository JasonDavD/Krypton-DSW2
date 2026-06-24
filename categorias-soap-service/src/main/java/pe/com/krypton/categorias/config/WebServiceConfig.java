package pe.com.krypton.categorias.config;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

/**
 * Config de Spring WS. El MessageDispatcherServlet atiende /ws/*; el WSDL se publica en
 * /ws/categorias.wsdl (el nombre del @Bean define la URL).
 */
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

    @Bean(name = "categorias") // -> /ws/categorias.wsdl
    public DefaultWsdl11Definition defaultWsdl11Definition(XsdSchema categoriaSchema) {
        DefaultWsdl11Definition def = new DefaultWsdl11Definition();
        def.setPortTypeName("CategoriasPort");
        def.setTargetNamespace("http://krypton.com/soap/categorias");
        def.setLocationUri("/ws");
        def.setSchema(categoriaSchema);
        return def;
    }

    @Bean
    public XsdSchema categoriaSchema() {
        return new SimpleXsdSchema(new ClassPathResource("wsdl/categoria.xsd"));
    }
}
