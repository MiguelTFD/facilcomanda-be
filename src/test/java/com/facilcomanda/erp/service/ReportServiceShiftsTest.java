package com.facilcomanda.erp.service;

import com.facilcomanda.erp.dto.ProductQuantityResponse;
import com.facilcomanda.erp.dto.ReportSummaryResponse;
import com.facilcomanda.erp.dto.ShiftSummaryResponse;
import com.facilcomanda.erp.model.enums.DayShift;
import com.facilcomanda.erp.model.enums.PaymentMethod;
import com.facilcomanda.erp.repository.ExpenseRepository;
import com.facilcomanda.erp.repository.InvoiceRepository;
import com.facilcomanda.erp.repository.MethodTotal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

/**
 * Feature 034 — CA1 a CA5: el reporte diario se parte en TURNO TARDE
 * (12:00–17:59), TURNO NOCHE (18:00–23:59) y OTROS (00:00–11:59) sobre la hora
 * de cobro de la factura.
 *
 * <p>Los repositorios están mockeados, así que lo que estos tests fijan es la
 * <b>ventana</b> con la que se consulta cada turno, no la clasificación que
 * hace PostgreSQL. Esa la garantizan las consultas semiabiertas de
 * {@code InvoiceRepository}, ya en producción desde la feature 022.</p>
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceShiftsTest {

    private static final Long ORG_ID = 7L;
    private static final LocalDate DAY = LocalDate.of(2026, 8, 12);

    private static final LocalDateTime MIDNIGHT = LocalDateTime.of(2026, 8, 12, 0, 0);
    private static final LocalDateTime NOON = LocalDateTime.of(2026, 8, 12, 12, 0);
    private static final LocalDateTime EVENING = LocalDateTime.of(2026, 8, 12, 18, 0);
    private static final LocalDateTime NEXT_MIDNIGHT = LocalDateTime.of(2026, 8, 13, 0, 0);

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private ReportService reportService;

    /**
     * Deja preparada una ventana completa. {@code lenient} porque no todos los
     * tests recorren las cuatro ventanas y los strict stubs de Mockito
     * fallarían por stubbing sin usar.
     */
    private void stubWindow(LocalDateTime start, LocalDateTime endExclusive,
            long ordersCount, String cashPaid, String yape, String credit,
            String change, String legacyCash, List<ProductQuantityResponse> items) {
        lenient().when(invoiceRepository.countInvoicesInPeriod(ORG_ID, start, endExclusive))
                .thenReturn(ordersCount);
        lenient().when(invoiceRepository.sumPaymentsByMethodInPeriod(ORG_ID, start, endExclusive))
                .thenReturn(List.of(
                        new MethodTotal(PaymentMethod.EFECTIVO, new BigDecimal(cashPaid)),
                        new MethodTotal(PaymentMethod.YAPE, new BigDecimal(yape)),
                        new MethodTotal(PaymentMethod.CREDITO, new BigDecimal(credit))));
        lenient().when(invoiceRepository.sumChangeAmountWithPaymentsInPeriod(ORG_ID, start, endExclusive))
                .thenReturn(new BigDecimal(change));
        lenient().when(invoiceRepository.sumOrderTotalWithoutPaymentsInPeriod(ORG_ID, start, endExclusive))
                .thenReturn(new BigDecimal(legacyCash));
        lenient().when(invoiceRepository.sumItemsByProductInPeriod(ORG_ID, start, endExclusive))
                .thenReturn(items);
    }

    private void stubFullDay() {
        stubWindow(MIDNIGHT, NEXT_MIDNIGHT, 10L, "53.00", "136.00", "20.00", "9.00", "10.00",
                List.of(new ProductQuantityResponse("Pollo al vino", 8L),
                        new ProductQuantityResponse("Taper", 6L)));
        stubWindow(NOON, EVENING, 5L, "30.00", "96.00", "0.00", "4.00", "0.00",
                List.of(new ProductQuantityResponse("Pollo al vino", 6L),
                        new ProductQuantityResponse("Taper", 2L)));
        stubWindow(EVENING, NEXT_MIDNIGHT, 4L, "15.00", "40.00", "20.00", "5.00", "10.00",
                List.of(new ProductQuantityResponse("Taper", 3L),
                        new ProductQuantityResponse("Pollo al vino", 2L)));
        stubWindow(MIDNIGHT, NOON, 1L, "8.00", "0.00", "0.00", "0.00", "0.00",
                List.of(new ProductQuantityResponse("Taper", 1L)));
        lenient().when(expenseRepository.sumAmountInPeriod(ORG_ID, DAY, DAY))
                .thenReturn(new BigDecimal("30.00"));
    }

    private ShiftSummaryResponse shiftOf(ReportSummaryResponse response, DayShift shift) {
        return response.shifts().stream()
                .filter(block -> block.shift() == shift)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No vino el bloque " + shift));
    }

    // --- CA1: invariante de partición ---

    @Test
    void getSummary_devuelveLosTresBloquesEnElOrdenTardeNocheOtros() {
        stubFullDay();

        assertThat(reportService.getSummary(ORG_ID, DAY, DAY).shifts())
                .extracting(ShiftSummaryResponse::shift)
                .containsExactly(DayShift.TARDE, DayShift.NOCHE, DayShift.OTROS);
    }

    @Test
    void getSummary_losTresTurnosSumanElTotalDiario() {
        stubFullDay();
        ReportSummaryResponse response = reportService.getSummary(ORG_ID, DAY, DAY);

        long ordersSum = response.shifts().stream().mapToLong(ShiftSummaryResponse::ordersCount).sum();
        long itemsSum = response.shifts().stream().mapToLong(ShiftSummaryResponse::itemsCount).sum();
        BigDecimal grossSum = response.shifts().stream()
                .map(ShiftSummaryResponse::grossTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(ordersSum).isEqualTo(response.ordersCount());
        assertThat(itemsSum).isEqualTo(response.itemsCount());
        assertThat(grossSum).isEqualByComparingTo(response.grossTotal());
    }

    @Test
    void getSummary_cadaMetodoDePagoSumaElTotalDiario() {
        stubFullDay();
        ReportSummaryResponse response = reportService.getSummary(ORG_ID, DAY, DAY);

        for (String method : List.of("EFECTIVO", "YAPE", "CREDITO")) {
            BigDecimal sum = response.shifts().stream()
                    .map(block -> block.collectedByMethod().get(method))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(sum)
                    .as("suma de %s en los tres turnos", method)
                    .isEqualByComparingTo(response.collectedByMethod().get(method));
        }
    }

    @Test
    void getSummary_cadaTurnoTraeSusPropiasCifras() {
        stubFullDay();
        ReportSummaryResponse response = reportService.getSummary(ORG_ID, DAY, DAY);

        ShiftSummaryResponse tarde = shiftOf(response, DayShift.TARDE);
        assertThat(tarde.ordersCount()).isEqualTo(5L);
        assertThat(tarde.itemsCount()).isEqualTo(8L);
        // 30.00 cobrado en efectivo − 4.00 de vuelto + 0.00 legacy
        assertThat(tarde.collectedByMethod().get("EFECTIVO")).isEqualByComparingTo("26.00");
        assertThat(tarde.grossTotal()).isEqualByComparingTo("122.00");

        ShiftSummaryResponse noche = shiftOf(response, DayShift.NOCHE);
        assertThat(noche.ordersCount()).isEqualTo(4L);
        assertThat(noche.itemsCount()).isEqualTo(5L);
        assertThat(noche.grossTotal()).isEqualByComparingTo("80.00");

        ShiftSummaryResponse otros = shiftOf(response, DayShift.OTROS);
        assertThat(otros.ordersCount()).isEqualTo(1L);
        assertThat(otros.grossTotal()).isEqualByComparingTo("8.00");
    }

    // --- CA2: fronteras exactas ---

    @Test
    void getSummary_lasFronterasDeCadaTurnoSonLasDeLaSpec() {
        stubFullDay();
        ReportSummaryResponse response = reportService.getSummary(ORG_ID, DAY, DAY);

        assertThat(shiftOf(response, DayShift.TARDE).fromTime()).isEqualTo(LocalTime.of(12, 0, 0));
        assertThat(shiftOf(response, DayShift.TARDE).toTime()).isEqualTo(LocalTime.of(17, 59, 59));
        assertThat(shiftOf(response, DayShift.NOCHE).fromTime()).isEqualTo(LocalTime.of(18, 0, 0));
        assertThat(shiftOf(response, DayShift.NOCHE).toTime()).isEqualTo(LocalTime.of(23, 59, 59));
        assertThat(shiftOf(response, DayShift.OTROS).fromTime()).isEqualTo(LocalTime.MIDNIGHT);
        assertThat(shiftOf(response, DayShift.OTROS).toTime()).isEqualTo(LocalTime.of(11, 59, 59));
    }

    @Test
    void getSummary_consultaCadaTurnoConSuVentanaSemiabierta() {
        stubFullDay();
        reportService.getSummary(ORG_ID, DAY, DAY);

        // El instante de corte pertenece al turno que empieza, nunca al que
        // termina: 12:00:00 es TARDE y 18:00:00 es NOCHE.
        org.mockito.Mockito.verify(invoiceRepository).countInvoicesInPeriod(ORG_ID, NOON, EVENING);
        org.mockito.Mockito.verify(invoiceRepository).countInvoicesInPeriod(ORG_ID, EVENING, NEXT_MIDNIGHT);
        org.mockito.Mockito.verify(invoiceRepository).countInvoicesInPeriod(ORG_ID, MIDNIGHT, NOON);
        org.mockito.Mockito.verify(invoiceRepository).countInvoicesInPeriod(ORG_ID, MIDNIGHT, NEXT_MIDNIGHT);
    }

    // --- CA3: solo el día ---

    @Test
    void getSummary_enUnRangoDeVariosDiasNoDevuelveTurnos() {
        LocalDate from = LocalDate.of(2026, 8, 10);
        LocalDate to = LocalDate.of(2026, 8, 16);
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime endExclusive = to.plusDays(1).atStartOfDay();

        stubWindow(start, endExclusive, 20L, "100.00", "50.00", "0.00", "0.00", "0.00",
                List.of(new ProductQuantityResponse("Taper", 4L)));
        lenient().when(expenseRepository.sumAmountInPeriod(ORG_ID, from, to)).thenReturn(BigDecimal.ZERO);

        assertThat(reportService.getSummary(ORG_ID, from, to).shifts()).isEmpty();
    }

    // --- CA4: reglas de la 022 dentro de cada ventana ---

    @Test
    void getSummary_elVueltoYLasFacturasLegacySeAplicanDentroDeSuTurno() {
        stubFullDay();
        ReportSummaryResponse response = reportService.getSummary(ORG_ID, DAY, DAY);

        // NOCHE: 15.00 de pagos en efectivo − 5.00 de vuelto + 10.00 de una
        // factura legacy sin filas de pago, contada por su orderTotal.
        assertThat(shiftOf(response, DayShift.NOCHE).collectedByMethod().get("EFECTIVO"))
                .isEqualByComparingTo("20.00");
        // El vuelto de la tarde no toca al efectivo de la noche.
        assertThat(shiftOf(response, DayShift.TARDE).collectedByMethod().get("EFECTIVO"))
                .isEqualByComparingTo("26.00");
    }

    @Test
    void getSummary_losEgresosYLaNetaSiguenSiendoSoloDiarios() {
        stubFullDay();
        ReportSummaryResponse response = reportService.getSummary(ORG_ID, DAY, DAY);

        assertThat(response.expensesTotal()).isEqualByComparingTo("30.00");
        // 210.00 − 30.00
        assertThat(response.netTotal()).isEqualByComparingTo("180.00");
    }

    // --- CA5: día vacío ---

    @Test
    void getSummary_unDiaSinFacturasDevuelveLosTresBloquesEnCero() {
        List<ProductQuantityResponse> nothing = List.of();
        stubWindow(MIDNIGHT, NEXT_MIDNIGHT, 0L, "0.00", "0.00", "0.00", "0.00", "0.00", nothing);
        stubWindow(NOON, EVENING, 0L, "0.00", "0.00", "0.00", "0.00", "0.00", nothing);
        stubWindow(EVENING, NEXT_MIDNIGHT, 0L, "0.00", "0.00", "0.00", "0.00", "0.00", nothing);
        stubWindow(MIDNIGHT, NOON, 0L, "0.00", "0.00", "0.00", "0.00", "0.00", nothing);
        lenient().when(expenseRepository.sumAmountInPeriod(ORG_ID, DAY, DAY)).thenReturn(BigDecimal.ZERO);

        ReportSummaryResponse response = reportService.getSummary(ORG_ID, DAY, DAY);

        assertThat(response.shifts()).hasSize(3);
        for (ShiftSummaryResponse block : response.shifts()) {
            assertThat(block.ordersCount()).isZero();
            assertThat(block.itemsCount()).isZero();
            assertThat(block.itemsByProduct()).isEmpty();
            assertThat(block.grossTotal()).isEqualByComparingTo("0.00");
            assertThat(block.collectedByMethod())
                    .containsOnlyKeys("EFECTIVO", "YAPE", "CREDITO");
        }
    }

    @Test
    void getSummary_cadaTurnoConsultaConElOrganizationIdDelToken() {
        stubFullDay();
        reportService.getSummary(ORG_ID, DAY, DAY);

        org.mockito.Mockito.verify(invoiceRepository, org.mockito.Mockito.times(4))
                .sumItemsByProductInPeriod(org.mockito.ArgumentMatchers.eq(ORG_ID),
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
