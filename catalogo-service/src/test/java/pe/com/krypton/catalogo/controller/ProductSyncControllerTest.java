package pe.com.krypton.catalogo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pe.com.krypton.catalogo.service.CatalogoSyncService;

/** Web slice del endpoint interno de sync. Sin Spring Security en el micro → acceso directo. */
@WebMvcTest(ProductSyncController.class)
class ProductSyncControllerTest {

    @Autowired MockMvc mvc;
    @MockBean CatalogoSyncService syncService;

    @Test
    void put_sync_returns_204_and_calls_service() throws Exception {
        String body = """
                {"sku":"KR-LAP-001","name":"Laptop","description":"d","price":4299.00,
                 "stock":12,"imageUrl":null,"active":false,"categoryId":1}
                """;

        mvc.perform(put("/internal/products/5").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());

        verify(syncService).sync(eq(5L), any());
    }
}
