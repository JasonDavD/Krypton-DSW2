package pe.com.krypton.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;

/**
 * Cliente SOAP hacia categorias-soap-service. El marshaller usa las clases JAXB generadas del
 * contrato (paquete pe.com.krypton.soap.ws). La URL del micro es fija (no va por Eureka/gateway),
 * parametrizable con CATEGORIAS_SOAP_URL para Docker.
 */
@Configuration
public class CategoriasSoapConfig {

    @Bean
    public Jaxb2Marshaller categoriasMarshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("pe.com.krypton.soap.ws");
        return marshaller;
    }

    @Bean
    public WebServiceTemplate categoriasWebServiceTemplate(
            Jaxb2Marshaller categoriasMarshaller,
            @Value("${CATEGORIAS_SOAP_URL:http://localhost:8095}") String baseUrl) {
        WebServiceTemplate template = new WebServiceTemplate();
        template.setMarshaller(categoriasMarshaller);
        template.setUnmarshaller(categoriasMarshaller);
        template.setDefaultUri(baseUrl + "/ws");
        return template;
    }
}
