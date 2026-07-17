package com.facilcomanda.erp.controller;

import com.facilcomanda.erp.model.enums.RoleName;
import com.facilcomanda.erp.security.AuthTestSupport;
import com.facilcomanda.erp.security.SecurityConfig;
import com.facilcomanda.erp.service.InvoiceService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.facilcomanda.erp.security.AuthTestSupport.as;
import static com.facilcomanda.erp.security.AuthTestSupport.assertAccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Autorización por rol de /api/invoices (feature 001): cobrar es exclusivo de
 * CAJERO (criterio 3: ADMIN recibe 403; SUPERADMIN también, hereda de ADMIN
 * que nunca cobra); historial de facturas solo ADMIN/SUPERADMIN (criterio 2:
 * MESERO recibe 403 en GET /api/invoices).
 */
@WebMvcTest(InvoiceController.class)
@Import({SecurityConfig.class, AuthTestSupport.SecurityFilterStubs.class})
class InvoiceControllerAuthorizationTest {

    private static final String VALID_PAYMENT_BODY =
            "{\"payments\": [{\"method\": \"EFECTIVO\", \"amount\": 50.00}]}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InvoiceService invoiceService;

    @ParameterizedTest
    @CsvSource({"MESERO,403", "COCINERO,403", "CAJERO,201", "ADMIN,403", "SUPERADMIN,403"})
    void chargeOrder(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(post("/api/invoices/orders/1/charge").with(as(role))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_PAYMENT_BODY)), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,403", "COCINERO,403", "CAJERO,403", "ADMIN,200", "SUPERADMIN,200"})
    void getInvoices(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(get("/api/invoices").with(as(role))), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,403", "COCINERO,403", "CAJERO,403", "ADMIN,200", "SUPERADMIN,200"})
    void getInvoiceById(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(get("/api/invoices/1").with(as(role))), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,403", "COCINERO,403", "CAJERO,403", "ADMIN,200", "SUPERADMIN,200"})
    void getInvoiceByOrderId(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(get("/api/invoices/orders/1").with(as(role))), expectedStatus);
    }
}
