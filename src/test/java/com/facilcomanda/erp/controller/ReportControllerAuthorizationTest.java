package com.facilcomanda.erp.controller;

import com.facilcomanda.erp.dto.ReportSummaryResponse;
import com.facilcomanda.erp.model.enums.RoleName;
import com.facilcomanda.erp.security.AuthTestSupport;
import com.facilcomanda.erp.security.SecurityConfig;
import com.facilcomanda.erp.service.ReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.facilcomanda.erp.security.AuthTestSupport.ORG_ID;
import static com.facilcomanda.erp.security.AuthTestSupport.as;
import static com.facilcomanda.erp.security.AuthTestSupport.assertAccess;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Feature 022 — CA3 (autorización {@code MANAGEMENT} de
 * {@code /api/reports/summary}) y contrato de la respuesta del resumen.
 */
@WebMvcTest(ReportController.class)
@Import({SecurityConfig.class, AuthTestSupport.SecurityFilterStubs.class})
class ReportControllerAuthorizationTest {

    private static final LocalDate FROM = LocalDate.of(2026, 7, 18);
    private static final LocalDate TO = LocalDate.of(2026, 7, 18);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @ParameterizedTest
    @CsvSource({"MESERO,403", "COCINERO,403", "CAJERO,200", "ADMIN,200", "SUPERADMIN,200"})
    void getSummary(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(get("/api/reports/summary")
                .param("from", "2026-07-18").param("to", "2026-07-18").with(as(role))), expectedStatus);
    }

    @Test
    void getSummary_devuelveElContratoCompletoDelResumen() throws Exception {
        Map<String, BigDecimal> collected = new LinkedHashMap<>();
        collected.put("EFECTIVO", new BigDecimal("135.00"));
        collected.put("YAPE", new BigDecimal("30.00"));
        collected.put("CREDITO", new BigDecimal("20.00"));
        when(reportService.getSummary(ORG_ID, FROM, TO)).thenReturn(new ReportSummaryResponse(
                FROM, TO, 4L, 8L,
                List.of(new com.facilcomanda.erp.dto.ProductQuantityResponse("Ceviche", 5L)),
                collected,
                new BigDecimal("185.00"), new BigDecimal("25.00"), new BigDecimal("160.00")));

        mockMvc.perform(get("/api/reports/summary")
                        .param("from", "2026-07-18").param("to", "2026-07-18").with(as(RoleName.CAJERO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-07-18"))
                .andExpect(jsonPath("$.to").value("2026-07-18"))
                .andExpect(jsonPath("$.ordersCount").value(4))
                .andExpect(jsonPath("$.itemsCount").value(8))
                .andExpect(jsonPath("$.itemsByProduct[0].productName").value("Ceviche"))
                .andExpect(jsonPath("$.itemsByProduct[0].quantity").value(5))
                .andExpect(jsonPath("$.collectedByMethod.EFECTIVO").value(135.00))
                .andExpect(jsonPath("$.collectedByMethod.YAPE").value(30.00))
                .andExpect(jsonPath("$.collectedByMethod.CREDITO").value(20.00))
                .andExpect(jsonPath("$.grossTotal").value(185.00))
                .andExpect(jsonPath("$.expensesTotal").value(25.00))
                .andExpect(jsonPath("$.netTotal").value(160.00));
    }
}
