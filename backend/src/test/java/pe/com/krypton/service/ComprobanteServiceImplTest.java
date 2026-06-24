package pe.com.krypton.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.com.krypton.dto.response.OrderResponse;
import pe.com.krypton.exception.ResourceNotFoundException;
import pe.com.krypton.model.User;
import pe.com.krypton.report.PdfExporter;
import pe.com.krypton.repository.UserRepository;
import pe.com.krypton.service.impl.ComprobanteServiceImpl;

/**
 * Unit test de ComprobanteServiceImpl. Colaboradores mockeados.
 * El comprobante de un pedido es del CLIENTE: la orden vive en pedidos-service (Feign),
 * el monolito valida ownership (userId del JWT vs userId de la orden) antes de generar el PDF.
 */
@ExtendWith(MockitoExtension.class)
class ComprobanteServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock AdminOrderService adminOrderService;
    @Mock PdfExporter pdfExporter;
    @InjectMocks ComprobanteServiceImpl service;

    private User user(Long id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        return u;
    }

    private OrderResponse orderOf(Long userId) {
        return new OrderResponse(1L, userId, Instant.now(), "CONFIRMADA",
                "BOLETA", "Cliente", "12345678",
                BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN, List.of());
    }

    @Test
    void should_generate_pdf_when_order_belongs_to_user() {
        when(userRepository.findByEmail("c@k.pe")).thenReturn(Optional.of(user(10L, "c@k.pe")));
        when(adminOrderService.getOrder(1L)).thenReturn(orderOf(10L));
        when(pdfExporter.exportComprobante(any())).thenReturn(new byte[]{ 0x25, 0x50, 0x44, 0x46 });

        byte[] pdf = service.generarComprobante("c@k.pe", 1L);

        assertThat(pdf).containsExactly(0x25, 0x50, 0x44, 0x46);
    }

    @Test
    void should_throw_not_found_when_order_belongs_to_another_user() {
        when(userRepository.findByEmail("c@k.pe")).thenReturn(Optional.of(user(10L, "c@k.pe")));
        when(adminOrderService.getOrder(1L)).thenReturn(orderOf(99L)); // orden de OTRO usuario

        assertThatThrownBy(() -> service.generarComprobante("c@k.pe", 1L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(pdfExporter, never()).exportComprobante(any());
    }

    @Test
    void should_throw_not_found_when_user_missing() {
        when(userRepository.findByEmail("x@k.pe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generarComprobante("x@k.pe", 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
