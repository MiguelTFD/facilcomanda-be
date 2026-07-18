package com.facilcomanda.erp.service;

import com.facilcomanda.erp.dto.ProductQuantityResponse;
import com.facilcomanda.erp.dto.ReportSummaryResponse;
import com.facilcomanda.erp.model.enums.PaymentMethod;
import com.facilcomanda.erp.repository.ExpenseRepository;
import com.facilcomanda.erp.repository.InvoiceRepository;
import com.facilcomanda.erp.repository.MethodTotal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resumen de ventas y egresos de un período (feature 022). Implementa las
 * "Definiciones de cálculo" de {@code spec/features/022-xx-reportes/spec.md},
 * que son la fuente de verdad de cada número. Toda la agregación ocurre en base
 * de datos: aquí solo se componen los totales ya sumados.
 */
@Service
public class ReportService {

    private static final int MONEY_SCALE = 2;

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
        BigDecimal expensesTotal = money(zeroIfNull(expenseRepository.sumAmountInPeriod(organizationId, from, to)));
        BigDecimal netTotal = money(grossTotal.subtract(expensesTotal));

        List<ProductQuantityResponse> itemsByProduct =
                invoiceRepository.sumItemsByProductInPeriod(organizationId, start, endExclusive);
        long itemsCount = itemsByProduct.stream()
                .mapToLong(item -> item.quantity() != null ? item.quantity() : 0L)
                .sum();

        return new ReportSummaryResponse(from, to, ordersCount, itemsCount, itemsByProduct,
                collectedByMethod, grossTotal, expensesTotal, netTotal);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
