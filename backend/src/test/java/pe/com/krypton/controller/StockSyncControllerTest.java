package pe.com.krypton.controller;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;
import pe.com.krypton.dto.request.StockSaleRequest;
import pe.com.krypton.security.JwtAuthenticationFilter;
import pe.com.krypton.service.StockSyncService;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Web slice: solo la capa HTTP de StockSyncController. Seguridad desactivada (addFilters=false);
 * el permit de /internal/** se valida en integración.
 */
@WebMvcTest(controllers = StockSyncController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class StockSyncControllerTest {

    @Autowired MockMvc mvc;
    @MockBean StockSyncService stockSyncService;

    @Test
    void should_return_204_and_delegate_to_service_when_sale_posted() throws Exception {
        String body = """
                {"reference":"123","items":[{"productId":10,"quantity":2}]}
                """;

        mvc.perform(post("/internal/stock/sale")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        ArgumentCaptor<StockSaleRequest> captor = ArgumentCaptor.forClass(StockSaleRequest.class);
        verify(stockSyncService).registerSale(captor.capture());
        StockSaleRequest req = captor.getValue();
        assertThat(req.reference()).isEqualTo("123");
        assertThat(req.items()).hasSize(1);
        assertThat(req.items().get(0).productId()).isEqualTo(10L);
        assertThat(req.items().get(0).quantity()).isEqualTo(2);
    }

    @Test
    void should_return_204_and_delegate_to_service_when_revert_posted() throws Exception {
        String body = """
                {"reference":"8","items":[{"productId":10,"quantity":1}]}
                """;

        mvc.perform(post("/internal/stock/revert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        ArgumentCaptor<StockSaleRequest> captor = ArgumentCaptor.forClass(StockSaleRequest.class);
        verify(stockSyncService).revertSale(captor.capture());
        assertThat(captor.getValue().reference()).isEqualTo("8");
        assertThat(captor.getValue().items().get(0).productId()).isEqualTo(10L);
    }
}
