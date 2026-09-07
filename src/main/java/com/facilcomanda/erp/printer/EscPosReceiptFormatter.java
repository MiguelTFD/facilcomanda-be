package com.facilcomanda.erp.printer;

import com.facilcomanda.erp.model.Order;
import com.facilcomanda.erp.model.OrderItem;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class EscPosReceiptFormatter {

    private static final Charset PRINTER_CHARSET = Charset.forName("CP437");
    private static final int PAPER_WIDTH_80MM = 48; 
    private static final String SEPARATOR = "-".repeat(PAPER_WIDTH_80MM) + "\n";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d/MM/yy, hh:mm a");

    public byte[] formatPreCuenta(Order order) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            out.write(EscPosCommands.INIT);

            out.write(EscPosCommands.ALIGN_CENTER);
            out.write(EscPosCommands.BOLD_ON);
            writeText(out, "La Casona Grill\n");
            out.write(EscPosCommands.BOLD_OFF);
            writeText(out, "RUC 10431561765\n");
            writeText(out, SEPARATOR);
            
            writeText(out, "PRE-CUENTA\n");
            writeText(out, SEPARATOR);

            out.write(EscPosCommands.ALIGN_LEFT);
            writeText(out, padLeftRight("Orden", "#" + String.format("%04d", order.getId())));
            writeText(out, padLeftRight("Fecha", order.getOrderDate().format(DATE_FORMATTER)));
            
            String tableName = order.getRestaurantTable() != null 
                ? order.getRestaurantTable().getName() 
                : "Barra";
            writeText(out, padLeftRight("Mesa", tableName));
            
            writeText(out, SEPARATOR);

            writeText(out, String.format(Locale.US, "%-4s %-24s %8s %9s\n", "CANT", "PRODUCTO", "P.U.", "TOTAL"));
            writeText(out, SEPARATOR);

            if (order.getOrderItems() != null) {
                for (OrderItem item : order.getOrderItems()) {
                    String product = item.getProduct().getName();
                    if (product.length() > 24) {
                        product = product.substring(0, 24);
                    }
                    
                    BigDecimal unitPrice = item.getSubtotal().divide(
                        BigDecimal.valueOf(item.getQuantity()), 2, RoundingMode.HALF_UP);
                    
                    writeText(out, String.format(Locale.US, "%4d %-24s %8.2f %9.2f\n", 
                        item.getQuantity(), 
                        product, 
                        unitPrice, 
                        item.getSubtotal()));
                    
                    String comments = item.getComments();
                    if (comments != null && !comments.trim().isEmpty()) {
                        comments = comments.trim();
                        
                        String indent = "       "; 
                        
                        int maxCommentLen = PAPER_WIDTH_80MM - indent.length();
                        if (comments.length() > maxCommentLen) {
                            comments = comments.substring(0, maxCommentLen);
                        }
                    
                        out.write(EscPosCommands.FONT_SMALL);
                        writeText(out, indent + comments + "\n");
                        out.write(EscPosCommands.FONT_NORMAL);
                    }
                }
            }
            writeText(out, SEPARATOR);

            out.write(EscPosCommands.BOLD_ON);
            writeText(out, padLeftRight("TOTAL", "S/ " + String.format(Locale.US, "%.2f", order.getTotal())));
            out.write(EscPosCommands.BOLD_OFF);
            writeText(out, SEPARATOR);

            out.write(EscPosCommands.ALIGN_CENTER);
            writeText(out, "\nNo es comprobante de pago. No acredita\ncancelacion.\n");
            
            writeText(out, "\n\n\n\n\n\n");
            out.write(EscPosCommands.CUT_PAPER);

            return out.toByteArray();
            
        } catch (IOException e) {
            throw new IllegalStateException("Error de I/O en memoria construyendo el payload ESC/POS", e);
        }
    }

    private void writeText(ByteArrayOutputStream out, String text) throws IOException {
        out.write(text.getBytes(PRINTER_CHARSET));
    }

    private String padLeftRight(String leftText, String rightText) {
        int spaceCount = PAPER_WIDTH_80MM - leftText.length() - rightText.length();
        if (spaceCount < 1) {
            spaceCount = 1; 
        }
        return leftText + " ".repeat(spaceCount) + rightText + "\n";
    }
}