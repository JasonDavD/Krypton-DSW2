package pe.com.krypton.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import pe.com.krypton.exception.ResourceNotFoundException;
import pe.com.krypton.security.JwtAuthenticationFilter;
import pe.com.krypton.service.ComprobanteService;

/**
 * Web slice para ComprobanteController. SecurityContext via @WithMockUser; JwtAuthenticationFilter
 * excluido. ComprobanteService mockeado. Cubre el contrato HTTP: PDF binario + Content-Disposition,
 * y la propagación de 404 (pedido inexistente o ajeno).
 */
@WebMvcTest(controllers = ComprobanteController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class ComprobanteControllerTest {

    @Autowired MockMvc mvc;
    @MockBean ComprobanteService comprobanteService;

    @Test
    @WithMockUser(username = "c@k.pe")
    void should_return_200_pdf_with_content_disposition() throws Exception {
        byte[] pdf = new byte[]{ 0x25, 0x50, 0x44, 0x46, 5, 6 }; // %PDF + relleno
        when(comprobanteService.generarComprobante(eq("c@k.pe"), eq(7L))).thenReturn(pdf);

        mvc.perform(get("/api/comprobantes/7"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", Matchers.containsString("comprobante_7.pdf")))
                .andExpect(content().bytes(pdf));
    }

    @Test
    @WithMockUser(username = "c@k.pe")
    void should_return_404_when_order_not_found_or_not_owner() throws Exception {
        when(comprobanteService.generarComprobante(eq("c@k.pe"), eq(99L)))
                .thenThrow(new ResourceNotFoundException("Pedido no encontrado: 99"));

        mvc.perform(get("/api/comprobantes/99"))
                .andExpect(status().isNotFound());
    }
}
