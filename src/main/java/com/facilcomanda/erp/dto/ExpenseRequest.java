package com.facilcomanda.erp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Alta o corrección de un egreso. Las reglas (monto &gt; 0, descripción
 * obligatoria, fecha requerida) se validan en {@code ExpenseService} con
 * excepciones de dominio; quien registra sale del token, no del cuerpo.
 */
public record ExpenseRequest(
        BigDecimal amount,
        String description,
        LocalDate expenseDate) {
}
