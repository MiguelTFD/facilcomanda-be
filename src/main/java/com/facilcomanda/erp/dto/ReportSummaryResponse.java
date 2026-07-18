package com.facilcomanda.erp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Resumen de un período (feature 022). {@code collectedByMethod} siempre trae
 * las tres claves EFECTIVO, YAPE y CREDITO, con cero cuando no hubo cobros por
 * ese método.
 */
public record ReportSummaryResponse(
        LocalDate from,
        LocalDate to,
        long ordersCount,
        long itemsCount,
        List<ProductQuantityResponse> itemsByProduct,
        Map<String, BigDecimal> collectedByMethod,
        BigDecimal grossTotal,
        BigDecimal expensesTotal,
        BigDecimal netTotal) {
}
