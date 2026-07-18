package com.facilcomanda.erp.controller;

import com.facilcomanda.erp.model.enums.RoleName;
import com.facilcomanda.erp.security.AuthTestSupport;
import com.facilcomanda.erp.security.SecurityConfig;
import com.facilcomanda.erp.service.ExpenseService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Feature 022 — CA3: los egresos son gestión operativa (regla
 * {@code MANAGEMENT}, decisión 5 de {@code roles.md}): CAJERO, ADMIN y
 * SUPERADMIN acceden; MESERO y COCINERO reciben 403.
 */
@WebMvcTest(ExpenseController.class)
@Import({SecurityConfig.class, AuthTestSupport.SecurityFilterStubs.class})
class ExpenseControllerAuthorizationTest {

    private static final String VALID_EXPENSE_BODY =
            "{\"amount\": 40.50, \"description\": \"Compra de gas\", \"expenseDate\": \"2026-07-18\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExpenseService expenseService;

    @ParameterizedTest
    @CsvSource({"MESERO,403", "COCINERO,403", "CAJERO,201", "ADMIN,201", "SUPERADMIN,201"})
    void createExpense(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(post("/api/expenses").with(as(role))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_EXPENSE_BODY)), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,403", "COCINERO,403", "CAJERO,200", "ADMIN,200", "SUPERADMIN,200"})
    void getExpenses(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(get("/api/expenses")
                .param("from", "2026-07-01").param("to", "2026-07-31").with(as(role))), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,403", "COCINERO,403", "CAJERO,200", "ADMIN,200", "SUPERADMIN,200"})
    void updateExpense(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(put("/api/expenses/1").with(as(role))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_EXPENSE_BODY)), expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({"MESERO,403", "COCINERO,403", "CAJERO,204", "ADMIN,204", "SUPERADMIN,204"})
    void deleteExpense(RoleName role, int expectedStatus) throws Exception {
        assertAccess(mockMvc.perform(delete("/api/expenses/1").with(as(role))), expectedStatus);
    }
}
