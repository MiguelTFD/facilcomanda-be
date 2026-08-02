package com.facilcomanda.erp.controller;

import com.facilcomanda.erp.dto.InvoiceItemResponse;
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
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.facilcomanda.erp.security.AuthTestSupport.as;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Feature 028 — contrato del ticket de venta: {@code items[]} viaja en la
 * respuesta de factura con sus cinco campos. El frontend lo consume para
 * imprimir; romper esta forma rompe el ticket.
 */
@WebMvcTest(InvoiceController.class)
@Import({SecurityConfig.class, AuthTestSupport.SecurityFilterStubs.class})
class InvoiceItemsContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InvoiceService invoiceService;

    private InvoiceResponse stubConItems() {
        return new InvoiceResponse(
                1L, "INV-7-42-20260801143205", 42L, 42L, 9L, "Mesa 3",
                new BigDecimal("55.00"), new BigDecimal("60.00"), new BigDecimal("5.00"),
                "EFECTIVO", LocalDateTime.of(2026, 8, 1, 14, 32), 3L, "cajero@facilcomanda.test", null,
                List.of(new PaymentEntryResponse(PaymentMethod.EFECTIVO, new BigDecimal("60.00"), null)),
                List.of(
                        new InvoiceItemResponse("Lomo saltado", 2, new BigDecimal("25.00"), new BigDecimal("50.00"), null),
                        new InvoiceItemResponse("Chicha morada", 1, new BigDecimal("5.00"), new BigDecimal("5.00"), "sin hielo")));
    }

    @Test
    void getInvoiceById_devuelveItemsConSusCincoCampos() throws Exception {
        when(invoiceService.getInvoiceById(any(), any())).thenReturn(stubConItems());

        mockMvc.perform(get("/api/invoices/1").with(as(RoleName.CAJERO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].productName").value("Lomo saltado"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].unitPrice").value(25.00))
                .andExpect(jsonPath("$.items[0].lineTotal").value(50.00))
                .andExpect(jsonPath("$.items[1].comments").value("sin hielo"));
    }

    @Test
    void getInvoices_devuelveItemsEnCadaFactura() throws Exception {
        when(invoiceService.getInvoices(any())).thenReturn(List.of(stubConItems()));

        mockMvc.perform(get("/api/invoices").with(as(RoleName.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].items.length()").value(2))
                .andExpect(jsonPath("$[0].items[0].productName").value("Lomo saltado"));
    }
}
