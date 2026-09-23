package com.facilcomanda.erp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record InvoicePaymentRequest(
        @NotEmpty @Valid List<PaymentEntryRequest> payments,
        @Size(max = 50) String invoiceType,
        @Size(max = 255) String notes) {
}
