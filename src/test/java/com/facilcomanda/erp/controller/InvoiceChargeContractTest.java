package com.facilcomanda.erp.controller;

import com.facilcomanda.erp.dto.InvoiceResponse;
import com.facilcomanda.erp.dto.PaymentEntryResponse;
import com.facilcomanda.erp.model.enums.PaymentMethod;
import com.facilcomanda.erp.model.enums.RoleName;
import com.facilcomanda.erp.security.AuthTestSupport;
import com.facilcomanda.erp.security.SecurityConfig;
import com.facilcomanda.erp.service.InvoiceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.facilcomanda.erp.security.AuthTestSupport.as;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Feature 020 — contrato del endpoint POST /api/invoices/orders/{id}/charge con
 * la forma nueva {@code {payments:[{method,amount,reference?}], notes?}} y la
 * respuesta con {@code payments[]}. Cubre además el rechazo 400 por Bean
 * Validation (lista vacía, monto no positivo).
 */
@WebMvcTest(InvoiceController.class)
@Import({SecurityConfig.class, AuthTestSupport.SecurityFilterStubs.class})
class InvoiceChargeContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InvoiceService invoiceService;

    @Test
    void chargeOrder_requestMultimetodo_devuelve201ConPaymentsYResumen() throws Exception {
        InvoiceResponse stub = new InvoiceResponse(
                1L, "INV-1-42-x", "BOLETA", 42L, 42L, 9L, "Mesa 3",
                new BigDecimal("52.00"), new BigDecimal("52.00"), new BigDecimal("0.00"),
                "MIXTO", LocalDateTime.now(), 3L, "cajero@facilcomanda.test", null,
                List.of(
                        new PaymentEntryResponse(PaymentMethod.EFECTIVO, new BigDecimal("20.00"), null),
                        new PaymentEntryResponse(PaymentMethod.CREDITO, new BigDecimal("32.00"), "Sr. Pérez")),
                List.of());
        when(invoiceService.chargeOrder(eq(42L), any(), any(), any())).thenReturn(stub);

        String body = "{\"payments\":["
                + "{\"method\":\"EFECTIVO\",\"amount\":20.00},"
                + "{\"method\":\"CREDITO\",\"amount\":32.00,\"reference\":\"Sr. Pérez\"}"
                + "],\"notes\":\"mesa 3\"}";

        mockMvc.perform(post("/api/invoices/orders/42/charge").with(as(RoleName.CAJERO))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentMethod").value("MIXTO"))
                .andExpect(jsonPath("$.payments.length()").value(2))
                .andExpect(jsonPath("$.payments[0].method").value("EFECTIVO"))
                .andExpect(jsonPath("$.payments[1].method").value("CREDITO"))
                .andExpect(jsonPath("$.payments[1].reference").value("Sr. Pérez"));
    }

    @Test
    void chargeOrder_listaDePagosVacia_devuelve400() throws Exception {
        mockMvc.perform(post("/api/invoices/orders/42/charge").with(as(RoleName.CAJERO))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"payments\":[]}"))
                .andExpect(status().isBadRequest());
        verify(invoiceService, never()).chargeOrder(any(), any(), any(), any());
    }

    @Test
    void chargeOrder_montoNoPositivo_devuelve400() throws Exception {
        String body = "{\"payments\":[{\"method\":\"EFECTIVO\",\"amount\":-5.00}]}";
        mockMvc.perform(post("/api/invoices/orders/42/charge").with(as(RoleName.CAJERO))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verify(invoiceService, never()).chargeOrder(any(), any(), any(), any());
    }
}
