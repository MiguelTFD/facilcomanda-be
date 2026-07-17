package com.facilcomanda.erp.dto;

import com.facilcomanda.erp.model.enums.PaymentMethod;
import java.math.BigDecimal;

public record PaymentEntryResponse(
        PaymentMethod method,
        BigDecimal amount,
        String reference) {
}
