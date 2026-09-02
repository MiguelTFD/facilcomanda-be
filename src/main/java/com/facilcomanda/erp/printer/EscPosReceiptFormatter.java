package com.facilcomanda.erp.printer;

import com.facilcomanda.erp.model.Order;
import com.facilcomanda.erp.model.OrderItem;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.format.DateTimeFormatter;

@Component
public class EscPosReceiptFormatter {

    // CP437 es nativo en NVRAM de impresoras para caracteres occidentales
    private static final Charset PRINTER_CHARSET = Charset.forName("CP437");
    private static final int PAPER_WIDTH_80MM = 42; 
    private static final String SEPARATOR = "-".repeat(PAPER_WIDTH_80MM) + "\n";
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d/MM/yy, hh:mm a");

    public byte[] formatPreCuenta(Order order) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // 1. Resetear buffer de impresora
            out.write(EscPosCommands.INIT);

            // 2. Cabecera (Alineación Central por Hardware)
            out.write(EscPosCommands.ALIGN_CENTER);
            out.write(EscPosCommands.BOLD_ON);
            writeText(out, "La Casona Grill\n");
            out.write(EscPosCommands.BOLD_OFF);
            writeText(out, "RUC 10431561765\n");
            writeText(out, SEPARATOR);
            
            writeText(out, "PRE-CUENTA\n");
            writeText(out, SEPARATOR);

            // 3. Metadatos (Alineación Izquierda por Hardware + Justificación en memoria)
            out.write(EscPosCommands.ALIGN_LEFT);
            writeText(out, padLeftRight("Orden", "#" + String.format("%04d", order.getId())));
            writeText(out, padLeftRight("Fecha", order.getOrderDate().format(DATE_FORMATTER)));
            
            String tableName = order.getRestaurantTable() != null 
                ? order.getRestaurantTable().getName() 
                : "Barra";
            writeText(out, padLeftRight("Mesa", tableName));
            
            writeText(out, SEPARATOR);

            // 4. Cabecera de Tabla de Productos
            // Distribución de 42 caracteres: CANT(4) + esp(1) + PROD(19) + esp(1) + PU(8) + esp(1) + TOTAL(8)
            writeText(out, String.format("%-4s %-19s %8s %8s\n", "CANT", "PRODUCTO", "P.U.", "TOTAL"));
            writeText(out, SEPARATOR);

            // 5. Detalles de Orden
            if (order.getOrderItems() != null) {
                for (OrderItem item : order.getOrderItems()) {
                    String product = item.getProduct().getName();
                    if (product.length() > 19) {
                        product = product.substring(0, 19);
                    }
                    
                    // Cálculo de precio unitario basado en subtotal y cantidad (o extraído del producto)
                    double unitPrice = item.getSubtotal().doubleValue() / item.getQuantity();
                    
                    writeText(out, String.format("%4d %-19s %8.2f %8.2f\n", 
                        item.getQuantity(), 
                        product, 
                        unitPrice, 
                        item.getSubtotal()));
                }
            }
            writeText(out, SEPARATOR);

            // 6. Totales (Justificado a los extremos)
            out.write(EscPosCommands.BOLD_ON);
            writeText(out, padLeftRight("TOTAL", "S/ " + String.format("%.2f", order.getTotal())));
            out.write(EscPosCommands.BOLD_OFF);
            writeText(out, SEPARATOR);

            // 7. Pie de página (Alineación Central)
            out.write(EscPosCommands.ALIGN_CENTER);
            writeText(out, "\nNo es comprobante de pago. No acredita\ncancelacion.\n");
            
            // Avance de papel de seguridad y corte
            writeText(out, "\n\n\n");
            out.write(EscPosCommands.CUT_PAPER);

            return out.toByteArray();
            
        } catch (IOException e) {
            throw new IllegalStateException("Error de I/O en memoria construyendo el payload ESC/POS", e);
        }
    }

    // --- Métodos de Utilidad (KISS & DRY) ---

    private void writeText(ByteArrayOutputStream out, String text) throws IOException {
        out.write(text.getBytes(PRINTER_CHARSET));
    }

    private String padLeftRight(String leftText, String rightText) {
        int spaceCount = PAPER_WIDTH_80MM - leftText.length() - rightText.length();
        if (spaceCount < 1) {
            spaceCount = 1; // Prevenir crash si las cadenas exceden el ancho
        }
        return leftText + " ".repeat(spaceCount) + rightText + "\n";
    }
}