package com.facilcomanda.erp.dto;

import java.math.BigDecimal;

/**
 * Una línea del ticket de venta (feature 028). {@code lineTotal} es el
 * {@code subtotal} congelado del {@code OrderItem}; {@code unitPrice} se deriva
 * de él y puede estar redondeado, por lo que el importe que manda es siempre
 * {@code lineTotal}.
 */
public record InvoiceItemResponse(
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        String comments) {
}
