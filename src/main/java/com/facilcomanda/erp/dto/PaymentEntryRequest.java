package com.facilcomanda.erp.dto;

import com.facilcomanda.erp.model.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PaymentEntryRequest(
        @NotNull PaymentMethod method,
        @NotNull @Positive BigDecimal amount,
        @Size(max = 255) String reference) {
}
