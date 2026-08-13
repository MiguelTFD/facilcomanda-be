package com.facilcomanda.erp.dto;

import com.facilcomanda.erp.model.enums.DayShift;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Bloque de un turno del reporte diario (feature 034). {@code fromTime} y
 * {@code toTime} son fronteras <b>inclusivas de presentación</b> (12:00 –
 * 17:59:59); el cálculo usa la ventana semiabierta equivalente. Se envían para
 * que el frontend no duplique los horarios.
 *
 * <p>No lleva {@code expensesTotal} ni {@code netTotal}: {@code
 * expenses.expense_date} es un {@code LocalDate} sin hora, así que los egresos
 * no se pueden repartir por turno (decisión 5 de la spec).</p>
 */
public record ShiftSummaryResponse(
        DayShift shift,
        LocalTime fromTime,
        LocalTime toTime,
        long ordersCount,
        long itemsCount,
        List<ProductQuantityResponse> itemsByProduct,
        Map<String, BigDecimal> collectedByMethod,
        BigDecimal grossTotal) {
}
