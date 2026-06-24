package pe.com.krypton.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.krypton.service.ComprobanteService;

/**
 * Descarga del comprobante (boleta/factura) en PDF de un pedido del cliente autenticado.
 * Bajo /api/comprobantes/** → cae en el catch-all del gateway hacia el monolito; el monolito
 * valida el JWT (anyRequest().authenticated()) y el service valida ownership del pedido.
 */
@RestController
@RequestMapping("/api/comprobantes")
@RequiredArgsConstructor
public class ComprobanteController {

    private final ComprobanteService comprobanteService;

    /** GET /api/comprobantes/{orderId} → 200 PDF (attachment) · 404 si no existe o es ajeno. */
    @GetMapping(value = "/{orderId}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> comprobante(@AuthenticationPrincipal UserDetails principal,
                                              @PathVariable Long orderId) {
        byte[] pdf = comprobanteService.generarComprobante(principal.getUsername(), orderId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment().filename("comprobante_" + orderId + ".pdf").build());
        headers.setContentLength(pdf.length);
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
