package com.facilcomanda.erp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceResponse(
        Long id,
        String invoiceNumber,
        Long orderId,
        Long orderNumber,
        Long restaurantTableId,
        String restaurantTableName,
        BigDecimal orderTotal,
        BigDecimal amountPaid,
        BigDecimal changeAmount,
        String paymentMethod,
        LocalDateTime paidAt,
        Long cashierUserId,
        String cashierEmail,
        String notes,
        List<PaymentEntryResponse> payments,
        List<InvoiceItemResponse> items) {
}
