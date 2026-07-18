package com.facilcomanda.erp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExpenseResponse(
        Long id,
        BigDecimal amount,
        String description,
        LocalDate expenseDate,
        Long registeredByUserId,
        String registeredByEmail,
        LocalDateTime createdAt) {
}
