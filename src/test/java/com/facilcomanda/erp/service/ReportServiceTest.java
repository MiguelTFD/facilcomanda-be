package com.facilcomanda.erp.service;

import com.facilcomanda.erp.dto.ProductQuantityResponse;
import com.facilcomanda.erp.dto.ReportSummaryResponse;
import com.facilcomanda.erp.model.enums.PaymentMethod;
import com.facilcomanda.erp.repository.ExpenseRepository;
import com.facilcomanda.erp.repository.InvoiceRepository;
import com.facilcomanda.erp.repository.MethodTotal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Feature 022 — CA1: cálculo del resumen según las "Definiciones de cálculo"
 * de {@code spec/features/022-xx-reportes/spec.md}. Facturas multimétodo,
 * factura legacy sin filas de pago (cuenta como EFECTIVO por su
 * {@code orderTotal}) y sobrepago en efectivo (el vuelto se descuenta del
 * EFECTIVO). Incluye el aislamiento multi-tenant (CA2): toda consulta se hace
 * con el {@code organizationId} del token.
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final Long ORG_ID = 7L;
    private static final LocalDate FROM = LocalDate.of(2026, 7, 13);
    private static final LocalDate TO = LocalDate.of(2026, 7, 19);

    /** El período es semiabierto: [from 00:00, to+1día 00:00). */
    private static final LocalDateTime START = LocalDateTime.of(2026, 7, 13, 0, 0);
    private static final LocalDateTime END_EXCLUSIVE = LocalDateTime.of(2026, 7, 20, 0, 0);

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        when(invoiceRepository.countInvoicesInPeriod(ORG_ID, START, END_EXCLUSIVE)).thenReturn(4L);
        when(invoiceRepository.sumPaymentsByMethodInPeriod(ORG_ID, START, END_EXCLUSIVE)).thenReturn(List.of(
                new MethodTotal(PaymentMethod.EFECTIVO, new BigDecimal("100.00")),
                new MethodTotal(PaymentMethod.YAPE, new BigDecimal("30.00")),
                new MethodTotal(PaymentMethod.CREDITO, new BigDecimal("20.00"))));
        when(invoiceRepository.sumChangeAmountWithPaymentsInPeriod(ORG_ID, START, END_EXCLUSIVE))
                .thenReturn(new BigDecimal("10.00"));
        when(invoiceRepository.sumOrderTotalWithoutPaymentsInPeriod(ORG_ID, START, END_EXCLUSIVE))
                .thenReturn(new BigDecimal("45.00"));
        when(invoiceRepository.sumItemsByProductInPeriod(ORG_ID, START, END_EXCLUSIVE)).thenReturn(List.of(
                new ProductQuantityResponse("Ceviche", 5L),
                new ProductQuantityResponse("Chicharrón", 3L)));
        when(expenseRepository.sumAmountInPeriod(ORG_ID, FROM, TO)).thenReturn(new BigDecimal("25.00"));
    }

    private ReportSummaryResponse summary() {
        return reportService.getSummary(ORG_ID, FROM, TO);
    }

    @Test
    void getSummary_efectivoDescuentaElVueltoYSumaLasFacturasLegacy() {
        // 100.00 cobrado en efectivo − 10.00 de vuelto + 45.00 de facturas sin
        // filas de pago (legacy, cuentan como EFECTIVO por su orderTotal).
        assertThat(summary().collectedByMethod().get("EFECTIVO")).isEqualByComparingTo("135.00");
    }

    @Test
    void getSummary_yapeYCreditoSeSumanTalComoSeRegistraron() {
        ReportSummaryResponse response = summary();
        assertThat(response.collectedByMethod().get("YAPE")).isEqualByComparingTo("30.00");
        assertThat(response.collectedByMethod().get("CREDITO")).isEqualByComparingTo("20.00");
    }

    @Test
    void getSummary_gananciaBrutaEsLaSumaDeLosTresMetodos() {
        // 135.00 + 30.00 + 20.00
        assertThat(summary().grossTotal()).isEqualByComparingTo("185.00");
    }

    @Test
    void getSummary_gananciaNetaDescuentaLosEgresosDelPeriodo() {
        ReportSummaryResponse response = summary();
        assertThat(response.expensesTotal()).isEqualByComparingTo("25.00");
        // 185.00 − 25.00
        assertThat(response.netTotal()).isEqualByComparingTo("160.00");
    }

    @Test
    void getSummary_cantidadDePlatosEsLaSumaDeItemsConDesglosePorProducto() {
        ReportSummaryResponse response = summary();
        assertThat(response.itemsCount()).isEqualTo(8L);
        assertThat(response.itemsByProduct()).containsExactly(
                new ProductQuantityResponse("Ceviche", 5L),
                new ProductQuantityResponse("Chicharrón", 3L));
    }

    @Test
    void getSummary_devuelveElRangoYElConteoDeOrdenesCobradas() {
        ReportSummaryResponse response = summary();
        assertThat(response.from()).isEqualTo(FROM);
        assertThat(response.to()).isEqualTo(TO);
        assertThat(response.ordersCount()).isEqualTo(4L);
    }

    @Test
    void getSummary_consultaSiempreConElOrganizationIdDelToken() {
        summary();

        verify(invoiceRepository).countInvoicesInPeriod(eq(ORG_ID), eq(START), eq(END_EXCLUSIVE));
        verify(invoiceRepository).sumPaymentsByMethodInPeriod(eq(ORG_ID), eq(START), eq(END_EXCLUSIVE));
        verify(invoiceRepository).sumChangeAmountWithPaymentsInPeriod(eq(ORG_ID), eq(START), eq(END_EXCLUSIVE));
        verify(invoiceRepository).sumOrderTotalWithoutPaymentsInPeriod(eq(ORG_ID), eq(START), eq(END_EXCLUSIVE));
        verify(invoiceRepository).sumItemsByProductInPeriod(eq(ORG_ID), eq(START), eq(END_EXCLUSIVE));
        verify(expenseRepository).sumAmountInPeriod(eq(ORG_ID), eq(FROM), eq(TO));
    }
}
