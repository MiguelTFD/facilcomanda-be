package com.facilcomanda.erp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record InvoicePaymentRequest(
        @NotNull @Positive BigDecimal amountPaid,
        @NotBlank @Size(max = 50) String paymentMethod,
        @Size(max = 255) String notes) {
}
