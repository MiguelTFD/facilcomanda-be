package com.facilcomanda.erp.service;

import com.facilcomanda.erp.dto.ProductQuantityResponse;
import com.facilcomanda.erp.dto.ReportSummaryResponse;
import com.facilcomanda.erp.dto.ShiftSummaryResponse;
import com.facilcomanda.erp.model.enums.DayShift;
import com.facilcomanda.erp.model.enums.PaymentMethod;
import com.facilcomanda.erp.repository.ExpenseRepository;
import com.facilcomanda.erp.repository.InvoiceRepository;
import com.facilcomanda.erp.repository.MethodTotal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resumen de ventas y egresos de un período (feature 022) y su partición por
 * turno en el reporte diario (feature 034). Implementa las "Definiciones de
 * cálculo" de {@code spec/features/022-xx-reportes/spec.md}, que siguen siendo
 * la fuente de verdad de cada número; la 034 solo las aplica sobre ventanas
 * más estrechas. Toda la agregación ocurre en base de datos.
 */
@Service
public class ReportService {

    private static final int MONEY_SCALE = 2;

    /**
     * Fronteras de los turnos (decisión 2 de la spec 034), fijas en código: hoy
     * hay un cliente y un horario. Hacerlas configurables por organización
     * exige la feature 029 y su pantalla de edición, que todavía no existe.
     */
    private static final LocalTime TARDE_START = LocalTime.of(12, 0);
    private static final LocalTime NOCHE_START = LocalTime.of(18, 0);

    private final InvoiceRepository invoiceRepository;
    private final ExpenseRepository expenseRepository;

    public ReportService(InvoiceRepository invoiceRepository, ExpenseRepository expenseRepository) {
        this.invoiceRepository = invoiceRepository;
        this.expenseRepository = expenseRepository;
    }

    @Transactional(readOnly = true)
    public ReportSummaryResponse getSummary(Long organizationId, LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime endExclusive = to.plusDays(1).atStartOfDay();

        WindowTotals day = aggregateWindow(organizationId, start, endExclusive);

        BigDecimal expensesTotal = money(zeroIfNull(expenseRepository.sumAmountInPeriod(organizationId, from, to)));
        BigDecimal netTotal = money(day.grossTotal().subtract(expensesTotal));

        // Los turnos solo tienen sentido en el reporte de un día: partir una
        // semana o un mes por franja horaria no significa nada para el negocio.
        List<ShiftSummaryResponse> shifts = from.equals(to)
                ? shiftsOf(organizationId, from)
                : List.of();

        return new ReportSummaryResponse(from, to, day.ordersCount(), day.itemsCount(), day.itemsByProduct(),
                day.collectedByMethod(), day.grossTotal(), expensesTotal, netTotal, shifts);
    }

    /**
     * Los tres bloques, en el orden en que se muestran (decisión 6): TARDE,
     * NOCHE y el residual OTROS. Las tres ventanas son disjuntas y su unión es
     * el día completo, así que todo agregado aditivo cuadra por construcción.
     */
    private List<ShiftSummaryResponse> shiftsOf(Long organizationId, LocalDate day) {
        LocalDateTime midnight = day.atStartOfDay();
        LocalDateTime noon = day.atTime(TARDE_START);
        LocalDateTime evening = day.atTime(NOCHE_START);
        LocalDateTime nextMidnight = day.plusDays(1).atStartOfDay();

        return List.of(
                shift(organizationId, DayShift.TARDE, noon, evening),
                shift(organizationId, DayShift.NOCHE, evening, nextMidnight),
                shift(organizationId, DayShift.OTROS, midnight, noon));
    }

    private ShiftSummaryResponse shift(Long organizationId, DayShift shift,
            LocalDateTime start, LocalDateTime endExclusive) {
        WindowTotals totals = aggregateWindow(organizationId, start, endExclusive);

        return new ShiftSummaryResponse(
                shift,
                start.toLocalTime(),
                // Frontera inclusiva de presentación: 18:00 exclusivo se
                // muestra como 17:59:59, y la medianoche siguiente como
                // 23:59:59.
                endExclusive.minusSeconds(1).toLocalTime(),
                totals.ordersCount(),
                totals.itemsCount(),
                totals.itemsByProduct(),
                totals.collectedByMethod(),
                totals.grossTotal());
    }

    /**
     * Agregados de una ventana semiabierta {@code [start, endExclusive)} sobre
     * {@code paidAt}. Es el cuerpo que la feature 022 tenía dentro de
     * {@code getSummary}, sin los egresos: estos van por {@code expenseDate},
     * que es un {@code LocalDate} y no admite ventanas horarias.
     */
    private WindowTotals aggregateWindow(Long organizationId, LocalDateTime start, LocalDateTime endExclusive) {
        long ordersCount = invoiceRepository.countInvoicesInPeriod(organizationId, start, endExclusive);

        Map<PaymentMethod, BigDecimal> byMethod = new EnumMap<>(PaymentMethod.class);
        for (MethodTotal total : invoiceRepository.sumPaymentsByMethodInPeriod(organizationId, start, endExclusive)) {
            byMethod.merge(total.method(), zeroIfNull(total.amount()), BigDecimal::add);
        }

        // El vuelto sale solo del efectivo; las facturas legacy (sin filas de
        // pago) cuentan como EFECTIVO por su orderTotal.
        BigDecimal change = zeroIfNull(
                invoiceRepository.sumChangeAmountWithPaymentsInPeriod(organizationId, start, endExclusive));
        BigDecimal legacyCash = zeroIfNull(
                invoiceRepository.sumOrderTotalWithoutPaymentsInPeriod(organizationId, start, endExclusive));

        BigDecimal cash = zeroIfNull(byMethod.get(PaymentMethod.EFECTIVO)).subtract(change).add(legacyCash);
        BigDecimal yape = zeroIfNull(byMethod.get(PaymentMethod.YAPE));
        BigDecimal credit = zeroIfNull(byMethod.get(PaymentMethod.CREDITO));

        Map<String, BigDecimal> collectedByMethod = new LinkedHashMap<>();
        collectedByMethod.put(PaymentMethod.EFECTIVO.name(), money(cash));
        collectedByMethod.put(PaymentMethod.YAPE.name(), money(yape));
        collectedByMethod.put(PaymentMethod.CREDITO.name(), money(credit));

        BigDecimal grossTotal = money(cash.add(yape).add(credit));

        List<ProductQuantityResponse> itemsByProduct =
                invoiceRepository.sumItemsByProductInPeriod(organizationId, start, endExclusive);
        long itemsCount = itemsByProduct.stream()
                .mapToLong(item -> item.quantity() != null ? item.quantity() : 0L)
                .sum();

        return new WindowTotals(ordersCount, itemsCount, itemsByProduct, collectedByMethod, grossTotal);
    }

    /** Agregados de una ventana. Interno: no forma parte del contrato HTTP. */
    private record WindowTotals(
            long ordersCount,
            long itemsCount,
            List<ProductQuantityResponse> itemsByProduct,
            Map<String, BigDecimal> collectedByMethod,
            BigDecimal grossTotal) {
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
