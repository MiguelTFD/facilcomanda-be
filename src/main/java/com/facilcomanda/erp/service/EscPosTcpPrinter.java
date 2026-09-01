package com.facilcomanda.erp.service;

import com.facilcomanda.erp.event.OrderCreatedEvent;
import com.facilcomanda.erp.model.Order;
import com.facilcomanda.erp.model.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;

@Component
public class EscPosTcpPrinter {

    private static final Logger log = LoggerFactory.getLogger(EscPosTcpPrinter.class);
    
    @Value("${printer.bar.ip:192.168.100.250}")
    private String printerIp;
    
    private final int PRINTER_PORT = 9100;

    // Escucha el evento SOLO si el commit en BD fue exitoso.
    // @Async obliga a Spring a usar un hilo secundario (fire and forget).
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        Order order = event.order();
        
        try (Socket socket = new Socket()) {
            // Socket de capa 4. Timeout de 3000ms para evitar bloqueos del hilo secundario.
            socket.connect(new InetSocketAddress(printerIp, PRINTER_PORT), 3000);
            
            OutputStream out = socket.getOutputStream();
            out.write(buildEscPosPayload(order));
            out.flush();
            
            log.info("Orden {} enviada exitosamente a impresora {}", order.getId(), printerIp);
        } catch (Exception e) {
            // El log registra la falla física, pero la orden ya es segura en la BD.
            log.error("Fallo de red ESC/POS hacia {}: {}", printerIp, e.getMessage());
        }
    }

    private byte[] buildEscPosPayload(Order order) {
        // CP437 es la página de códigos por defecto en NVRAM para terminales ESC/POS
        Charset charset = Charset.forName("CP437"); 
        StringBuilder sb = new StringBuilder();

        // Inicializar hardware (0x1B 0x40)
        sb.append((char) 27).append((char) 64);

        // Cabecera
        sb.append("TICKET DE ORDEN #").append(order.getId()).append("\n");
        sb.append("------------------------------------------\n");
        sb.append(String.format("%-4s %-25s %8s\n", "CANT", "PRODUCTO", "PRECIO"));
        sb.append("------------------------------------------\n");

        // Detalle de Items
        if (order.getOrderItems() != null) {
            for (OrderItem item : order.getOrderItems()) {
                String product = item.getProduct().getName();
                if (product.length() > 25) {
                    product = product.substring(0, 25);
                }
                sb.append(String.format("%-4d %-25s %8.2f\n", 
                    item.getQuantity(), 
                    product, 
                    item.getSubtotal()));
            }
        }
        
        sb.append("------------------------------------------\n");
        sb.append(String.format("%-30s %8.2f\n", "TOTAL:", order.getTotal()));
        
        // Avance de papel para evitar que la guillotina corte el texto
        sb.append("\n\n\n\n");
        
        // Comando de corte total (0x1D 0x56 0x00)
        sb.append((char) 29).append((char) 86).append((char) 0);

        return sb.toString().getBytes(charset);
    }
}