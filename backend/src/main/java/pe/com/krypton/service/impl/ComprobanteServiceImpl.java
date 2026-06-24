package pe.com.krypton.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.com.krypton.dto.response.OrderResponse;
import pe.com.krypton.exception.ResourceNotFoundException;
import pe.com.krypton.model.User;
import pe.com.krypton.report.PdfExporter;
import pe.com.krypton.repository.UserRepository;
import pe.com.krypton.service.AdminOrderService;
import pe.com.krypton.service.ComprobanteService;

/**
 * Comprobante del cliente: trae la orden de pedidos-service (Feign vía {@link AdminOrderService}),
 * valida que pertenezca al usuario autenticado y reusa {@link PdfExporter} para el PDF.
 * Si la orden es de otro usuario responde 404 (anti-IDOR), igual que el detalle del pedido.
 */
@Service
@RequiredArgsConstructor
public class ComprobanteServiceImpl implements ComprobanteService {

    private final UserRepository userRepository;
    private final AdminOrderService adminOrderService;
    private final PdfExporter pdfExporter;

    @Override
    public byte[] generarComprobante(String email, Long orderId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + email));
        OrderResponse order = adminOrderService.getOrder(orderId);
        if (!user.getId().equals(order.userId())) {
            throw new ResourceNotFoundException("Pedido no encontrado: " + orderId);
        }
        return pdfExporter.exportComprobante(order);
    }
}
